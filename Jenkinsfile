pipeline {
    agent any

    environment {
        SERVER_IP = "rhel8.redhat.com" // IP address of the JBoss VM
        HOST_USER = "jboss"        // Host machine SSH username
        HOST_PASSWORD = "redhat" // Host machine SSH password
        JBOSS_USER = "admin"     // JBoss management username
        JBOSS_PASSWORD = "redhat" // JBoss management password
        JBOSS_HOME = "/home/jboss/EAP-7.4.0" // JBoss EAP home directory on the JBoss VM
        WAR_FILE = "erythu-java-app/target/erythu-java-app-1.0-SNAPSHOT.war" // Path to .war file in Jenkins workspace
        REMOTE_WAR_PATH = "/home/jboss" // Target directory on JBoss VM for .war file
        JBOSS_PORT = "13190"                // JBoss management console port
    }

    stages {

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
                    sh 'mvn clean install -f erythu-java-app/pom.xml'
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

        stage('Test') {
            steps {
                echo "DEBUG: Running unit tests with Maven..."
                sh 'mvn test -f erythu-java-app/pom.xml'
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