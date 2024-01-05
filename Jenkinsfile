// Change Snapshot to your own DevCloud Artifactory repo name
def Snapshot = 'PROPEL'
library "security-ci-commons-shared-lib"
def NODE = nodeDetails("java")

def digitalGridArtServer = Artifactory.server('Digital-Artifactory')
def ARTIFACTORY_SERVER_URL = digitalGridArtServer.getUrl()

pipeline {
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: '1', artifactNumToKeepStr: '1', daysToKeepStr: '5', numToKeepStr: '10'))
    }
    agent {
        docker {
            image "${NODE['IMAGE']}"
            label "${NODE['LABEL']}"
        }
    }
    stages {
        stage('Test') {
            steps {
                checkout scm
                dir('spring-filters-config') {
                    git branch: 'master', changelog: false, credentialsId: 'github.build.ge.com', poll: false, url: 'https://github.build.ge.com/predix/spring-filters-config.git'
                }
                sh '''#!/bin/bash -ex
                    source spring-filters-config/set-env-metering-filter.sh
                    unset NON_PROXY_HOSTS
                    unset HTTPS_PROXY_PORT
                    unset HTTPS_PROXY_HOST

                    mvn clean verify -B -s spring-filters-config/mvn_settings_noproxy.xml
                '''
                // Test reports
            }
            post {
                always {
                    junit '**/surefire-reports/junitreports/TEST*.xml'
                    step([$class: 'JacocoPublisher', maximumBranchCoverage: '90', maximumInstructionCoverage: '90'])
                }
            }
        }
        stage('Publish Artifacts') {
            when {
                beforeAgent true
                branch 'master'
            }
            environment {
                DEPLOY_CREDS = credentials('DIGITAL_GRID_ARTIFACTORY_CREDENTIALS')
                MAVEN_CENTRAL_STAGING_PROFILE_ID=credentials('MAVEN_CENTRAL_STAGING_PROFILE_ID')
            }
            steps {
                script {
                    ARTIFACTORY_REPO = 'pgog-fss-iam-uaa-mvn'
                    sh """#!/bin/bash -ex
                        apk update
                        apk add --no-cache gnupg
                        gpg --version
                        ln -s ${WORKSPACE} /working-dir

                        mvn clean deploy -B -s spring-filters-config/mvn_settings_noproxy.xml \\
                        -DaltDeploymentRepository=artifactory.uaa.releases::default::${ARTIFACTORY_SERVER_URL}/${ARTIFACTORY_REPO} \\
                        -Dartifactory.user=${DEPLOY_CREDS_USR} \\
                        -Dartifactory.password=${DEPLOY_CREDS_PSW} \\
                        -D skipTests -e

                        #Deploy/Release to maven central repository
                        mvn clean deploy -B -P release -s spring-filters-config/mvn_settings_noproxy.xml \\
                        -D gpg.homedir=/working-dir/spring-filters-config/gnupg \\
                        -D stagingProfileId=$MAVEN_CENTRAL_STAGING_PROFILE_ID \\
                        -D skipTests -e
                    """
                }
            }
        }
    }
}
