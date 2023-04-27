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
        string(name: "BRANCH", defaultValue: "${env.gitlabBranch != null ? env.gitlabBranch : "dev"}", description: "")

        booleanParam(name: "CHECK_SCHEMAS_ONLY", defaultValue: false, description: '')
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    manager.addShortText(params.BRANCH)
                }
                updateGitlabCommitStatus name: "build", state: "running"

                git url: "git@gitlab.just-ai.com:mpl-public/mpl-java-sdk.git",
                        branch: "${params.BRANCH}",
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
                    sh """mvn versions:set -DnewVersion=${params.BRANCH}-SNAPSHOT"""
                    sh """mvn clean deploy"""
                }
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
