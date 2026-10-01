pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        ECR_REGISTRY = '123456789012.dkr.ecr.us-east-1.amazonaws.com/banking'
        EKS_CLUSTER = 'banking-eks-prod'
        SONAR_QUBE_SERVER = 'SonarQube-Enterprise'
    }

    tools {
        jdk 'JDK-21'
        maven 'Maven-3.9'
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        ansiColor('xterm')
    }

    stages {
        stage('Checkout Source') {
            steps {
                checkout scm
            }
        }

        stage('Compile & Unit Test') {
            steps {
                sh 'mvn clean test jacoco:report -B'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: '**/target/site/jacoco/**', fingerprint: true
                }
            }
        }

        stage('SonarQube Static Analysis') {
            steps {
                withSonarQubeEnv(env.SONAR_QUBE_SERVER) {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=banking-platform -Dsonar.qualitygate.wait=true'
                }
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        }
                    }
                }
            }
        }

        stage('OWASP Dependency Check') {
            steps {
                sh 'mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 || true'
            }
        }

        stage('Build & Push Docker Images') {
            when {
                branch 'main'
            }
            steps {
                script {
                    def services = ['api-gateway', 'account-service', 'payment-service', 'exchange-rate-service']
                    sh 'aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}'
                    
                    for (service in services) {
                        sh """
                            docker build --build-arg MODULE_NAME=${service} -t ${ECR_REGISTRY}/banking-${service}:${BUILD_NUMBER} .
                            docker push ${ECR_REGISTRY}/banking-${service}:${BUILD_NUMBER}
                        """
                    }
                }
            }
        }

        stage('Deploy to Dev') {
            when {
                branch 'develop'
            }
            steps {
                sh """
                    aws eks update-kubeconfig --name ${EKS_CLUSTER}-dev --region ${AWS_REGION}
                    helm upgrade --install banking-platform ./helm/banking-platform \
                        --namespace dev --create-namespace \
                        --set image.tag=${BUILD_NUMBER} --wait
                """
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: "Approve deployment of release ${BUILD_NUMBER} to Production EKS?"
                sh """
                    aws eks update-kubeconfig --name ${EKS_CLUSTER} --region ${AWS_REGION}
                    helm upgrade --install banking-platform ./helm/banking-platform \
                        --namespace production --create-namespace \
                        --set image.tag=${BUILD_NUMBER} \
                        --wait --timeout 10m
                """
            }
        }
    }

    post {
        failure {
            echo "CI/CD Pipeline failed. Alerting engineering team."
        }
        success {
            echo "CI/CD Pipeline successfully executed and deployed."
        }
    }
}
