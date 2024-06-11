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
        ///
        @Grab('com.fasterxml.jackson.core:jackson-databind:2.12.3')
        import com.fasterxml.jackson.databind.ObjectMapper

        pipeline {
            agent any

            environment {
                REPO_URL = 'https://gitlab.just-ai.com/ml-platform-pub/mlp-java-sdk.git'
                NEXUS_OPEN_URL = 'https://nexus-open.just-ai.com'
                NEXUS_INTERNAL_URL = 'https://nexus.just-ai.com'
                RELEASE_BRANCH = 'release'
                LAST_VERSION_FILE = 'last-released-version'
            }

            stages {
                stage('Clone Repository') {
                    steps {
                        git branch: env.RELEASE_BRANCH, url: env.REPO_URL
                    }
                }

                stage('Check Version') {
                    steps {
                        script {
                        // находим файл last-released-version в каталоге
                            def lastVersionData = readFile(env.LAST_VERSION_FILE)

                            def objectMapper = new ObjectMapper()
                            def versionData = objectMapper.readValue(lastVersionData, Map.class)

                            def currentVersion = versionData['currentVersion']
                            def previousVersions = versionData['previousVersions']

                            if (previousVersions.contains(currentVersion)) {
                                error "Version ${currentVersion} already in release"
                            } else {
                                currentBuild.description = "New version to release: ${currentVersion}"
                            }
                        }
                    }
                }

                stage('Create Release Branch') {
                    steps {
                        script {
                            def lastVersionData = readFile(env.LAST_VERSION_FILE)
                            def objectMapper = new ObjectMapper()
                            def versionData = objectMapper.readValue(lastVersionData, Map.class)

                            def currentVersion = versionData['currentVersion']
                            sh "git checkout -b release-${currentVersion}"
                        }
                    }
                }

                stage('Update Version') {
                    steps {
                        script {
                            def lastVersionData = readFile(env.LAST_VERSION_FILE)
                            def objectMapper = new ObjectMapper()
                            def versionData = objectMapper.readValue(lastVersionData, Map.class)

                            def currentVersion = versionData['currentVersion']
                            def releaseVersion = "release-${currentVersion}"

                             sh "mvn versions:set -DnewVersion=${releaseVersion}"
                        }
                    }
                }

                stage('Deploy to Nexus Open') {
                    steps {
                        script {
                            sh "mvn clean deploy -P nexus-open-release"
                        }
                    }
                }

                stage('Deploy to Internal Nexus') {
                    steps {
                        script {
                            sh "mvn clean deploy -P internal-release"
                        }
                    }
                }

                stage('Tag Release') {
                    steps {
                        script {
                            def lastVersionData = readFile(env.LAST_VERSION_FILE)
                            def objectMapper = new ObjectMapper()
                            def versionData = objectMapper.readValue(lastVersionData, Map.class)

                            def currentVersion = versionData['currentVersion']
                            def releaseVersion = "release-${currentVersion}"
                            sh "git tag ${releaseVersion}"
                        }
                    }
                }

                stage('Update Previous Versions') {
                    steps {
                        script {
                            def lastVersionData = readFile(env.LAST_VERSION_FILE)
                            def objectMapper = new ObjectMapper()
                            def versionData = objectMapper.readValue(lastVersionData, Map.class)

                            def currentVersion = versionData['currentVersion']
                            def releaseVersion = "release-${currentVersion}"
                            versionData['previousVersions'].add(currentVersion)

                            def updatedJson = objectMapper.writeValueAsString(versionData)
                            writeFile file: env.LAST_VERSION_FILE, text: updatedJson
                            sh "git add ${env.LAST_VERSION_FILE}"
                            sh "git commit -m 'Add ${currentVersion} to previous versions'"
                            sh "git push origin release-${currentVersion}"
                        }
                    }
                }
            }
        }


        ///

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
