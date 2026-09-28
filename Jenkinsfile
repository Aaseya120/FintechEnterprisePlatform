pipeline {
    agent any

    tools {
        maven 'Maven-3.9.6'
        jdk 'JDK-17'
    }

    environment {
        DOCKER_REGISTRY = 'registry.enterprise.internal'
        APP_VERSION = "1.0.0-${BUILD_NUMBER}"
        SONAR_QUBE_ENV = 'EnterpriseSonarServer'
    }

    stages {
        stage('Checkout') {
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
                    jacoco execPattern: '**/target/jacoco.exec', classPattern: '**/target/classes'
                }
            }
        }

        stage('SonarQube Static Analysis') {
            steps {
                withSonarQubeEnv("${SONAR_QUBE_ENV}") {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=fintech-enterprise-platform'
                }
            }
        }

        stage('Quality Gate Check') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline aborted due to Quality Gate failure: ${qg.status}"
                        }
                    }
                }
            }
        }

        stage('Build & Push Docker Containers') {
            when {
                branch 'main'
            }
            steps {
                script {
                    sh """
                        docker build -f docker/Dockerfile.gateway -t ${DOCKER_REGISTRY}/api-gateway:${APP_VERSION} .
                        docker build -f docker/Dockerfile.order -t ${DOCKER_REGISTRY}/order-service:${APP_VERSION} .
                        docker build -f docker/Dockerfile.payment -t ${DOCKER_REGISTRY}/payment-service:${APP_VERSION} .
                        
                        docker push ${DOCKER_REGISTRY}/api-gateway:${APP_VERSION}
                        docker push ${DOCKER_REGISTRY}/order-service:${APP_VERSION}
                        docker push ${DOCKER_REGISTRY}/payment-service:${APP_VERSION}
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                branch 'main'
            }
            steps {
                withKubeConfig([credentialsId: 'k8s-cluster-creds']) {
                    sh '''
                        kubectl apply -f k8s/namespace.yaml
                        kubectl apply -f k8s/config-and-secrets.yaml
                        kubectl apply -f k8s/api-gateway-deployment.yaml
                        kubectl apply -f k8s/order-service-deployment.yaml
                        kubectl apply -f k8s/payment-service-deployment.yaml
                        kubectl apply -f k8s/ingress.yaml
                        kubectl rollout status deployment/order-service -n fintech-platform --timeout=120s
                    '''
                }
            }
        }
    }

    post {
        failure {
            echo "CI/CD Pipeline Failed! Check build logs and notify team."
        }
        success {
            echo "CI/CD Pipeline Finished Successfully!"
        }
    }
}
