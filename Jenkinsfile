pipeline {
    agent any

    stages {

        stage('Check Vault Health') {
            steps {
                echo "Running vault health check.."
                script {
                    def response = sh(
                        script: "curl -s --connect-timeout 5 -o /dev/null -w '%{http_code}' http://13.233.124.117:8200/v1/sys/health",
                        returnStdout: true
                    ).trim()

                    if (response != '200' && response != '429') {
                        error "Vault is not reachable or healthy. Status code: ${response}"
                    }
                }
            }
        }

        stage('Fetch Secrets from Vault') {
            steps {
                withVault(
                    vaultSecrets: [
                        [path: 'secret/data/erythu-java-app', secretValues: [
                            [envVar: 'HOST_PASSWORD', vaultKey: 'HOST_PASSWORD'],
                            [envVar: 'HOST_USER', vaultKey: 'HOST_USER'],
                            [envVar: 'JBOSS_HOME', vaultKey: 'JBOSS_HOME'],
                            [envVar: 'JBOSS_PASSWORD', vaultKey: 'JBOSS_PASSWORD'],
                            [envVar: 'JBOSS_PORT', vaultKey: 'JBOSS_PORT'],
                            [envVar: 'JBOSS_USER', vaultKey: 'JBOSS_USER'],
                            [envVar: 'REMOTE_WAR_PATH', vaultKey: 'REMOTE_WAR_PATH'],
                            [envVar: 'SERVER_IP', vaultKey: 'SERVER_IP'],
                            [envVar: 'SONAR_HOST_URL', vaultKey: 'SONAR_HOST_URL'],
                            [envVar: 'SONAR_TOKEN', vaultKey: 'SONAR_TOKEN'],
                            [envVar: 'WAR_FILE', vaultKey: 'WAR_FILE']
                        ]]
                    ]
                ) {
                    script {
                        echo "DEBUG: Secrets fetched successfully from Vault."
                        // Optional debug logs (ensure secrets are not printed)
                    }
                }
            }
        }

        stage('Cleanup Workspace') {
            steps {
                script {
                    echo 'Cleaning up the Jenkins workspace...'
                    deleteDir()
                }
            }
        }

        stage('Checkout Code') {
            steps {
                echo "DEBUG: Checking out the source code from SCM..."
                checkout scm
            }
        }

        stage('Verify Workspace') {
            steps {
                sh 'ls -R'
            }
        }

        stage('Build WAR File') {
            steps {
                script {
                    echo "DEBUG: Starting Maven build to generate the WAR file..."
                    sh 'mvn clean install -f pom.xml'
                    echo "DEBUG: Maven build completed. Checking for WAR file existence..."
                    sh '''
                    if [ -f ${WAR_FILE} ]; then
                        echo "DEBUG: WAR file generated successfully at ${WAR_FILE}."
                    else
                        echo "ERROR: WAR file not found! Build might have failed."
                        exit 1
                    fi
                    '''
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    // Define credentials for SonarQube host and token
                    withCredentials([
                        string(credentialsId: 'sonar', variable: 'SONAR_TOKEN'),
                        string(credentialsId: 'sonar-host-url', variable: 'SONAR_HOST_URL')
                    ]) {
                        // Wrap the analysis in withSonarQubeEnv
                        withSonarQubeEnv('SonarQube') { // Replace 'SonarQube' with your SonarQube server name in Jenkins configuration
                            sh """
                                mvn clean verify sonar:sonar \
                                    -Dsonar.projectKey=erythu-java-app \
                                    -Dsonar.host.url=$SONAR_HOST_URL \
                                    -Dsonar.login=$SONAR_TOKEN
                            """
                        }
                    }
                }
            }
        }

        stage('Quality Gate Check') {
            steps {
                script {
                    // Wait for SonarQube Quality Gate result
                    timeout(time: 5, unit: 'MINUTES') { // Adjust timeout as needed
                        def qualityGate = waitForQualityGate()
                        if (qualityGate.status != 'OK') {
                            error "Pipeline aborted due to SonarQube quality gate failure: ${qualityGate.status}"
                        }
                    }
                }
            }
        }

        stage('Transfer WAR File to JBoss') {
            steps {
                script {
                    echo "DEBUG: Starting transfer of WAR file to JBoss server..."
                    sh """
                        # Validate environment variables
                        echo "DEBUG: Validating environment variables..." || exit 1;

                        if [ -z "${HOST_PASSWORD}" ] || [ -z "${WAR_FILE}" ] || [ -z "${HOST_USER}" ] || [ -z "${SERVER_IP}" ] || [ -z "${REMOTE_WAR_PATH}" ]; then
                            echo "ERROR: One or more required environment variables are undefined.";
                            exit 1;
                        fi

                        echo "DEBUG: All environment variables are set.";

                        # Verify sshpass installation
                        if ! command -v sshpass >/dev/null 2>&1; then
                            echo "ERROR: sshpass is not installed!";
                            exit 1;
                        fi

                        # Verify WAR file existence
                        if [ ! -f ${WAR_FILE} ]; then
                            echo "ERROR: WAR file not found at ${WAR_FILE}.";
                            exit 1;
                        fi

                        # Transfer WAR file
                        echo "DEBUG: Transferring WAR file to target server...";
                        sshpass -p '${HOST_PASSWORD}' scp -o StrictHostKeyChecking=no ${WAR_FILE} ${HOST_USER}@${SERVER_IP}:${REMOTE_WAR_PATH} || {
                            echo "ERROR: SCP transfer failed!";
                            exit 1;
                        }

                        echo "DEBUG: WAR file transferred successfully.";
                    """
                }
            }
        }

        stage('Deploy WAR on JBoss') {
            steps {
                script {
                    echo "DEBUG: Starting deployment of WAR file on JBoss server..."
                    sh """
                    sshpass -p '${HOST_PASSWORD}' ssh -o StrictHostKeyChecking=no ${HOST_USER}@${SERVER_IP} \
                    '${JBOSS_HOME}/bin/jboss-cli.sh --connect --controller=${SERVER_IP}:${JBOSS_PORT} --user=${JBOSS_USER} --password=${JBOSS_PASSWORD} --command="deploy ${REMOTE_WAR_PATH}/erythu-java-app-1.0-SNAPSHOT.war --force"' || {
                        echo "ERROR: Deployment failed!"
                        exit 1
                    }
                    echo "DEBUG: Deployment completed successfully."
                    """
                }
            }
        }
    }

    post {
        success {
            echo "DEBUG: Pipeline executed successfully! Deployment completed."
        }
        failure {
            echo "ERROR: Pipeline execution failed. Check the logs for details."
        }
    }
}
