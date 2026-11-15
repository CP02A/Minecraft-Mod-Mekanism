#!/usr/bin/env groovy
// Standard pipeline shared by all Fabric 26.3 port projects (the same file in every repo).
//
// Every build: compile the Fabric jar, compile the NeoForge jar (only if the project has a `buildForge` task),
// then run the non-UI tests (`nonUiTest`: unit tests, headless game tests, checks).
// The UI tests (`uiTest`) open a game window, so they only run when the RUN_UI_TESTS parameter is ticked
// ("Build with Parameters"). They need an agent with a desktop session: on Windows the agent must run as a logged-in
// user (not as a service); on Linux xvfb-run is used when it is installed.
//
// The agent needs a JDK on PATH/JAVA_HOME to start Gradle; the Java 25 toolchain is downloaded by Gradle itself.

def gradle(String args) {
    if (isUnix()) {
        sh "./gradlew ${args}"
    } else {
        bat "gradlew.bat ${args}"
    }
}

def gradleStatus(String args) {
    return isUnix() ? sh(script: "./gradlew ${args}", returnStatus: true)
                    : bat(script: "gradlew.bat ${args}", returnStatus: true)
}

pipeline {
    agent any

    parameters {
        booleanParam(name: 'RUN_UI_TESTS', defaultValue: false,
                description: 'Also run the UI tests (opens the game window, needs a desktop session)')
    }

    environment {
        GRADLE_ARGS = '--no-daemon --stacktrace --console=plain'
    }

    options {
        timeout(time: 3, unit: 'HOURS')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'chmod +x gradlew'
                    }
                    // `help --task` fails when the task doesn't exist, so projects without a NeoForge build skip it
                    env.HAS_FORGE = gradleStatus("${env.GRADLE_ARGS} -q help --task buildForge") == 0 ? 'true' : 'false'
                    echo "NeoForge build present: ${env.HAS_FORGE}"
                }
            }
        }

        stage('Compile Fabric') {
            steps {
                script { gradle("${env.GRADLE_ARGS} buildFabric") }
            }
        }

        stage('Compile NeoForge') {
            when { environment name: 'HAS_FORGE', value: 'true' }
            steps {
                script { gradle("${env.GRADLE_ARGS} buildForge") }
            }
        }

        stage('Non-UI tests') {
            steps {
                script { gradle("${env.GRADLE_ARGS} --continue nonUiTest") }
            }
        }

        stage('UI tests') {
            when { expression { params.RUN_UI_TESTS } }
            steps {
                script {
                    if (isUnix() && sh(script: 'command -v xvfb-run', returnStatus: true) == 0) {
                        sh "xvfb-run -a ./gradlew ${env.GRADLE_ARGS} uiTest"
                    } else {
                        gradle("${env.GRADLE_ARGS} uiTest")
                    }
                }
            }
        }
    }

    post {
        always {
            junit testResults: '**/build/test-results/**/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'build/dist/**/*.jar', allowEmptyArchive: true, fingerprint: true
            archiveArtifacts artifacts: '**/run*/**/screenshots/**/*.png', allowEmptyArchive: true
        }
    }
}
