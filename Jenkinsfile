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
    parameters {
        string(name: "BRANCH", defaultValue: "dev", description: "")
        booleanParam(name: "CHECK_SCHEMAS_ONLY", defaultValue: false, description: '')
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    manager.addShortText("${env.gitlabBranch != null ? env.gitlabBranch : params.BRANCH}")
                    echo "${env.gitlabBranch}"
                }
                updateGitlabCommitStatus name: "build", state: "running"

                git url: "git@gitlab.just-ai.com:ml-platform-pub/mlp-java-sdk.git",
                        branch: "${env.gitlabBranch != null ? env.gitlabBranch : params.BRANCH}",
                        credentialsId: 'bitbucket_key'
            }
        }

        stage('Update spec') {
            steps {
                script {
                    sh("./mlp-specs/update.sh")

                    def hasChanges = !sh(returnStdout: true, script: 'git status -s mlp-specs').trim().isEmpty()

                    if (hasChanges) {
                        sh("git commit -m 'Automatic update API spec from CI' mlp-specs")
                        sh("git push")
                    }

                    env.NEED_REBUILD = hasChanges || !params.CHECK_SCHEMAS_ONLY
                }
            }
        }

        stage('Build with maven') {
            when {
                expression { env.NEED_REBUILD == 'true' }
            }
            steps {
                withMaven(maven: 'Maven 3.5', jdk: '11') {
                    sh """mvn versions:set -DnewVersion=${env.gitlabBranch != null ? env.gitlabBranch : params.BRANCH}-SNAPSHOT"""
                    sh """mvn clean deploy"""
                    sh """mvn deploy -P nexus-open"""
                }
            }
        }

        stage('Rebuild MLP Services') {
            when {
                expression { params.BRANCH == 'dev' || params.BRANCH == 'stable' || params.BRANCH == 'release' }
            }
            steps {
                parallel (
                    "build mlp-ai-proxy" : {
                        build job: "mlp-ai-proxy/${params.BRANCH}", wait: false
                    },
                    "build mlp-aimyvoice-proxy-service" : {
                        build job: "mlp-aimyvoice-proxy-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-censorship-bot" : {
                        build job: "mlp-censorship-bot-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-chat-service" : {
                        build job: "mlp-chat-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-chit-chat-service" : {
                        build job: "mlp-chit-chat-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-cloud-dalle-service" : {
                        build job: "mlp-cloud-dalle-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-cloud-whisper-service" : {
                        build job: "mlp-cloud-whisper-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-cross-validation-service" : {
                        build job: "mlp-cross-validation-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-faq-service" : {
                        build job: "mlp-faq-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-gpt-mock" : {
                        build job: "mlp-gpt-mock-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-intents" : {
                        build job: "mlp-intents-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-justgpt-facade" : {
                        build job: "mlp-justgpt-facade-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-kaldi-asr-service" : {
                        build job: "mlp-kaldi-asr-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-loadtest-service" : {
                        build job: "mlp-loadtest-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-service-proxy" : {
                        build job: "mlp-service-proxy-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-summary-service" : {
                        build job: "mlp-summary-service-build/${params.BRANCH}", wait: false
                    },
                    "build mlp-vectorize-service" : {
                        build job: "mlp-vectorize-service-build/${params.BRANCH}", wait: false
                    },
                )
            }
        }
    }
    post {
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
