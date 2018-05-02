// Change Snapshot to your own DevCloud Artifactory repo name
def Snapshot = 'PROPEL'
library "security-ci-commons-shared-lib"
def NODE = nodeDetails("java")

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

                    mvn clean verify -s spring-filters-config/mvn_settings_noproxy.xml
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
                branch 'master'
            }
            environment {
                DEPLOY_CREDS = credentials('uaa-predix-artifactory-upload-credentials')
                MAVEN_CENTRAL_STAGING_PROFILE_ID=credentials('MAVEN_CENTRAL_STAGING_PROFILE_ID')
            }
            steps {
                sh '''#!/bin/bash -ex
                    apk update
                    apk add --no-cache gnupg
                    gpg --version
                    ln -s ${WORKSPACE} /working-dir

                    mvn clean deploy -B -s spring-filters-config/mvn_settings_noproxy.xml \\
                    -DaltDeploymentRepository=artifactory.releases::default::https://devcloud.swcoe.ge.com/artifactory/MAAXA-MVN \\
                    -Dartifactory.password=${DEPLOY_CREDS_PSW} \\
                    -D skipTests -e

                    #Deploy/Release to maven central repository
                    mvn clean deploy -B -P release -s spring-filters-config/mvn_settings_noproxy.xml \\
                    -D gpg.homedir=/working-dir/spring-filters-config/gnupg \\
                    -D stagingProfileId=$MAVEN_CENTRAL_STAGING_PROFILE_ID \\
                    -D skipTests -e
                '''
            }
        }
    }
}
