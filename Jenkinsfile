pipeline {
    agent any
    tools {
        jdk 'JDK21'
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'dev',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/gus-p3/U1_Banco_API.git'
            }
        }
        stage('Ver Directorio') {
            steps {
                sh 'ls -la'
                sh 'java -version'
            }
        }
        stage('Permisos Gradle') {
            steps {
                sh 'chmod +x gradlew'
            }
        }
        stage('Build & Test') {
            steps {
                sh '''
                    ./gradlew clean test jacocoTestReport --no-daemon
                '''
            }
        }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarServer') {
                    sh '''
                        ./gradlew sonar \
                        -Dsonar.projectKey=Api_Gesto_Pago \
                        -Dsonar.projectName="Api_Gesto_Pago" \
                        -Dsonar.host.url=http://host.docker.internal:9000 \
                        -Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml
                    '''
                }
            }
        }
        stage('Security Scan (SAST) - Semgrep') {
            steps {
                sh '''
                    mkdir -p reportes
                    docker run --rm \
                    --network bridge \
                    -v "${WORKSPACE}":/src \
                    -w /src \
                    semgrep/semgrep \
                    semgrep scan \
                    --config=auto \
                    --json \
                    --output=reportes/semgrep.json \
                    .
                '''
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'reportes/semgrep.json',
            allowEmptyArchive: true,
            fingerprint: true
        }
    }
}
