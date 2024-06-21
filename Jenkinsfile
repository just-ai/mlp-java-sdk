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
                    RESULT_BRANCH = env.gitlabBranch != null ? env.gitlabBranch : params.BRANCH
                    manager.addShortText("${RESULT_BRANCH}")
                    echo "${env.gitlabBranch}"
                }
                updateGitlabCommitStatus name: "build", state: "running"

                git url: "git@gitlab.just-ai.com:ml-platform-pub/mlp-java-sdk.git",
                        branch: "${RESULT_BRANCH}",
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
                    sh """mvn versions:set -DnewVersion=${RESULT_BRANCH}-SNAPSHOT"""
                    sh """mvn clean deploy"""
                    sh """mvn deploy -P nexus-open-snapshot"""
                }
            }
        }
        ////
        pipeline {
            agent any

            parameters {
                string(name: 'RELEASE_BRANCH', defaultValue: 'release', description: 'Ветка на основе которой выпускаем версию')
                string(name: 'NEW_VERSION', defaultValue: '1.0.0', description: 'Новая версия из 3х цифр')
            }

            environment {
                RELEASE_DEPLOY_PROFILE = 'nexus-open-release'
                GITLAB_REPO = 'https://gitlab.just-ai.com/ml-platform-pub/mlp-java-sdk.git'
                GITHUB_REPO = 'https://github.com/just-ai/mlp-java-sdk.git'
            }

            stages {
                stage('Checkout') {
                    steps {
                        git branch: "${params.RELEASE_BRANCH}", url: env.GITLAB_REPO
                    }
                }

                stage('Set Version') {
                    steps {
                        sh "mvn versions:set -DnewVersion=${params.NEW_VERSION} -DgenerateBackupPoms=false"
                    }
                }

                stage('Deploy') {
                    steps {
                        sh "mvn clean deploy -P ${env.RELEASE_DEPLOY_PROFILE}"
                    }
                }

                stage('Update Version Info') {
                    steps {
                        script {
                            def versionInfo = readJSON file: 'last-released-version.json'

                            versionInfo.currentVersion = params.NEW_VERSION
                            versionInfo.versions.add(0, params.NEW_VERSION)

                            writeJSON file: 'last-released-version.json', json: versionInfo, pretty: 4
                        }
                    }
                }

                stage('Revert to Snapshot Version') {
                    steps {
                        sh "mvn versions:set -DnewVersion=dev-SNAPSHOT -DgenerateBackupPoms=false"
                    }
                }

                stage('Commit and Tag') {
                    steps {
                        sh """
                            git add last-released-version.json
                            git commit -m "update release version to ${params.NEW_VERSION}"
                            git tag release-${params.NEW_VERSION}
                        """
                    }
                }

                stage('Push Changes') {
                    steps {
                        sh """
                            git push ${env.GITLAB_REPO} ${params.RELEASE_BRANCH}
                            git push ${env.GITLAB_REPO} --tags
                            git push ${env.GITHUB_REPO} ${params.RELEASE_BRANCH}
                            git push ${env.GITHUB_REPO} --tags
                        """
                    }
                }
            }

            post {
                always {
                    cleanWs()
                }
            }
        }
        ////
        stage('Rebuild MLP Services') {
            when {
                expression { RESULT_BRANCH in ['dev','stable','release'] }
            }
            steps {
                parallel (
                    "build mlp-ai-proxy" : {
                        build job: "mlp-ai-proxy/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-aimyvoice-proxy-service" : {
                        build job: "mlp-aimyvoice-proxy-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-censorship-bot" : {
                        build job: "mlp-censorship-bot-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-chat-service" : {
                        build job: "mlp-chat-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-chit-chat-service" : {
                        build job: "mlp-chit-chat-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-cloud-dalle-service" : {
                        build job: "mlp-cloud-dalle-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-cloud-whisper-service" : {
                        build job: "mlp-cloud-whisper-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-cross-validation-service" : {
                        build job: "mlp-cross-validation-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-faq-service" : {
                        build job: "mlp-faq-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-gpt-mock" : {
                        build job: "mlp-gpt-mock-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-intents" : {
                        build job: "mlp-intents-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-justgpt-facade" : {
                        build job: "mlp-justgpt-facade-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-kaldi-asr-service" : {
                        build job: "mlp-kaldi-asr-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-loadtest-service" : {
                        build job: "mlp-loadtest-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-service-proxy" : {
                        build job: "mlp-service-proxy-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-summary-service" : {
                        build job: "mlp-summary-service-build/${RESULT_BRANCH}", wait: false
                    },
                    "build mlp-vectorize-service" : {
                        build job: "mlp-vectorize-service-build/${RESULT_BRANCH}", wait: false
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
