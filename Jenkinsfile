pipeline {
    options {
        gitLabConnection("gitlab just-ai")
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
    }
    agent {
        label 'caila-dev-cloud-agent'
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    addBadge(cssClass: "badge-text--background badge-text--bordered", text: env.BRANCH_NAME)
                }
                updateGitlabCommitStatus name: "build", state: "running"

                git url: "git@gitlab.just-ai.com:ml-platform-pub/mlp-java-sdk.git",
                        branch: "${env.BRANCH_NAME}",
                        credentialsId: 'bitbucket_key'
            }
        }

        stage('Build with maven') {
            steps {
                script {
                    def pomVersion = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    if (env.BRANCH_NAME == 'release') {
                        withMaven(maven: 'Maven 3.5', jdk: '17') {
                            sh "mvn clean deploy -U -P nexus-open-release"
                        }
                    } else {
                        withMaven(maven: 'Maven 3.5', jdk: '17') {
                            sh "mvn versions:set -DnewVersion=${env.BRANCH_NAME}-${pomVersion}-SNAPSHOT"
                            sh "mvn clean deploy -U -P nexus-open-snapshot"
                        }
                    }
                }
            }
        }

        stage('Rebuild MLP Services') {
            when {
                expression { env.BRANCH_NAME in ['release'] }
            }
            steps {
                parallel (
                    "build mlp-ai-proxy" : {
                        build job: "mlp-ai-proxy/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-aimyvoice-proxy-service" : {
                        build job: "mlp-aimyvoice-proxy-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-censorship-bot" : {
                        build job: "mlp-censorship-bot-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-chat-service" : {
                        build job: "mlp-chat-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-chit-chat-service" : {
                        build job: "mlp-chit-chat-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-cloud-dalle-service" : {
                        build job: "mlp-cloud-dalle-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-cloud-whisper-service" : {
                        build job: "mlp-cloud-whisper-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-cross-validation-service" : {
                        build job: "mlp-cross-validation-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-faq-service" : {
                        build job: "mlp-faq-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-gpt-mock" : {
                        build job: "mlp-gpt-mock-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-intents" : {
                        build job: "mlp-intents-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-justgpt-facade" : {
                        build job: "mlp-justgpt-facade-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-kaldi-asr-service" : {
                        build job: "mlp-kaldi-asr-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-loadtest-service" : {
                        build job: "mlp-loadtest-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-service-proxy" : {
                        build job: "mlp-service-proxy-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-summary-service" : {
                        build job: "mlp-summary-service-build/${env.BRANCH_NAME}", wait: false
                    },
                    "build mlp-vectorize-service" : {
                        build job: "mlp-vectorize-service-build/${env.BRANCH_NAME}", wait: false
                    }
                        ,
                )
            }
        }
    }
    post {
        always {
            cleanWs()
        }
        failure {
            updateGitlabCommitStatus name: "build", state: "failed"
        }
        success {
            updateGitlabCommitStatus name: "build", state: "success"
        }
        unstable {
            updateGitlabCommitStatus name: "build", state: "failed"
        }
    }
}
