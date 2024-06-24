pipeline {
    agent {
        label 'caila-dev-cloud-agent'
    }

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
                withMaven(maven: 'Maven 3.5', jdk: '11') {
                    sh "mvn versions:set -DnewVersion=${params.NEW_VERSION} -DgenerateBackupPoms=false"
                }
            }
        }

        stage('Deploy') {
            steps {
                withMaven(maven: 'Maven 3.5', jdk: '11') {
                    sh "mvn clean deploy -P ${env.RELEASE_DEPLOY_PROFILE}"
                }
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
                withMaven(maven: 'Maven 3.5', jdk: '11') {
                    sh "mvn versions:set -DnewVersion=dev-SNAPSHOT -DgenerateBackupPoms=false"
                }
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
