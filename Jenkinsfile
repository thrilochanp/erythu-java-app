pipeline {
    agent any

    // environment {
    //     // Placeholder for Vault secrets injection
    // }

    stages {
        stage('Fetch Secrets from Vault') {
            steps {
                script {
                    withVault(
                        configuration: [
                            vaultUrl: 'http://127.0.0.1:8200',
                            vaultCredentialsId: 'vault-token'
                        ], 
                        vaultSecrets: [
                            [
                                path: 'secret/erythu-java-app', 
                                secretValues: [
                                    [envVar: 'SERVER_IP', vaultKey: 'server_ip'], 
                                    [envVar: 'HOST_USER', vaultKey: 'host_user'], 
                                    [envVar: 'HOST_PASSWORD', vaultKey: 'host_password'], 
                                    [envVar: 'JBOSS_USER', vaultKey: 'jboss_user'], 
                                    [envVar: 'JBOSS_PASSWORD', vaultKey: 'jboss_password'], 
                                    [envVar: 'JBOSS_HOME', vaultKey: 'jboss_home'], 
                                    [envVar: 'WAR_FILE', vaultKey: 'war_file'], 
                                    [envVar: 'REMOTE_WAR_PATH', vaultKey: 'remote_war_path'], 
                                    [envVar: 'JBOSS_PORT', vaultKey: 'jboss_port'], 
                                    [envVar: 'SONAR_HOST_URL', vaultKey: 'sonar_host_url'], 
                                    [envVar: 'SONAR_TOKEN', vaultKey: 'sonar_token']
                                ]
                            ]
                        ]
                    ) {
                        echo 'Secrets fetched from Vault and injected into environment variables.'
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
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                    sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=erythu-java-app -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_TOKEN'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    timeout(time: 1, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
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
                        echo "DEBUG: Validating environment variables...";

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
