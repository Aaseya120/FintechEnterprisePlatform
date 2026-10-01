package com.banking.batch.config;

import com.banking.batch.model.ClearingRecordDto;
import com.banking.batch.model.StagingTransaction;
import com.banking.batch.procedure.OracleProcedureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;
import java.time.LocalDate;
import java.util.UUID;

@Configuration
public class ClearingBatchJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ClearingBatchJobConfig.class);
    private static final int CHUNK_SIZE = 2500;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final OracleProcedureService oracleProcedureService;

    public ClearingBatchJobConfig(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  DataSource dataSource,
                                  OracleProcedureService oracleProcedureService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
        this.oracleProcedureService = oracleProcedureService;
    }

    /**
     * High-speed streaming FlatFileItemReader for processing million-record clearing feeds.
     * Uses buffered streaming I/O with constant O(1) heap consumption.
     */
    @Bean
    @StepScope
    public FlatFileItemReader<ClearingRecordDto> clearingFileReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        String path = (filePath != null && !filePath.isBlank()) ? filePath : "data/sample_clearing_feed.csv";

        return new FlatFileItemReaderBuilder<ClearingRecordDto>()
                .name("clearingFileReader")
                .resource(new FileSystemResource(new File(path)))
                .delimited()
                .names("externalTxnRef", "sourceAccount", "targetAccount", "amount", "currency", "channel", "settlementDate")
                .linesToSkip(1) // Skip CSV Header row
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(ClearingRecordDto.class);
                }})
                .build();
    }

    @Bean
    public ItemProcessor<ClearingRecordDto, StagingTransaction> clearingItemProcessor() {
        return dto -> {
            // Validation & Cleansing
            if (dto.amount() == null || dto.sourceAccount() == null) {
                return null; // Skip invalid records
            }

            LocalDate settlementDate = (dto.settlementDate() != null)
                    ? LocalDate.parse(dto.settlementDate())
                    : LocalDate.now();

            return new StagingTransaction(
                    UUID.randomUUID().toString(),
                    dto.externalTxnRef(),
                    dto.sourceAccount(),
                    dto.targetAccount(),
                    dto.amount(),
                    dto.currency().toUpperCase(),
                    dto.channel(),
                    settlementDate,
                    1L
            );
        };
    }

    /**
     * Native JDBC Batch Writer executing batch inserts directly into Oracle/Postgres staging table.
     */
    @Bean
    public JdbcBatchItemWriter<StagingTransaction> stagingBatchWriter() {
        String sql = """
            INSERT INTO stg_clearing_transactions (
                id, external_txn_ref, source_account, target_account, amount, currency, channel, settlement_date, processing_status, batch_job_id, created_at
            ) VALUES (
                :id, :externalTxnRef, :sourceAccount, :targetAccount, :amount, :currency, :channel, :settlementDate, :processingStatus, :batchJobId, :createdAt
            )
            """;

        return new JdbcBatchItemWriterBuilder<StagingTransaction>()
                .dataSource(dataSource)
                .sql(sql)
                .beanMapped()
                .assertUpdates(false)
                .build();
    }

    @Bean
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("spring-batch-worker-");
        executor.setConcurrencyLimit(10); // 10 parallel threads per chunk
        return executor;
    }

    /**
     * Step 1: Ingest huge flat file into Staging Table using multi-threaded chunks.
     */
    @Bean
    public Step fileToStagingStep(FlatFileItemReader<ClearingRecordDto> reader,
                                  ItemProcessor<ClearingRecordDto, StagingTransaction> processor,
                                  JdbcBatchItemWriter<StagingTransaction> writer) {
        return new StepBuilder("fileToStagingStep", jobRepository)
                .<ClearingRecordDto, StagingTransaction>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(batchTaskExecutor())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100) // Skip corrupted lines gracefully
                .build();
    }

    /**
     * Step 2: Invoke Oracle 19c PL/SQL Package procedure to bulk-reconcile staged data inside DB.
     */
    @Bean
    public Step oraclePlSqlReconciliationStep() {
        Tasklet tasklet = (contribution, chunkContext) -> {
            Long jobId = chunkContext.getStepContext().getStepExecution().getJobExecution().getId();
            log.info("Starting Step 2: Triggering Oracle PL/SQL Bulk Reconcile for Job ID: {}", jobId);
            int reconciledCount = oracleProcedureService.processStagingBatch(jobId);
            log.info("Oracle PL/SQL procedure successfully merged and reconciled {} records.", reconciledCount);
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("oraclePlSqlReconciliationStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    /**
     * Main High-Volume Clearing & Reconciliation Job
     */
    @Bean
    public Job clearingSettlementJob(Step fileToStagingStep, Step oraclePlSqlReconciliationStep) {
        return new JobBuilder("clearingSettlementJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        log.info(">>> STARTING SPRING BATCH JOB: {} [Id: {}]",
                                jobExecution.getJobInstance().getJobName(), jobExecution.getId());
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        long durationMs = jobExecution.getEndTime() != null
                                ? (jobExecution.getEndTime().getNano() - jobExecution.getStartTime().getNano()) / 1_000_000
                                : 0;
                        log.info("<<< COMPLETED SPRING BATCH JOB: {} [Status: {} Duration: {}ms]",
                                jobExecution.getJobInstance().getJobName(), jobExecution.getStatus(), durationMs);
                    }
                })
                .start(fileToStagingStep)
                .next(oraclePlSqlReconciliationStep)
                .build();
    }
}
