node {
    try {
        // requires AnsiColor plugin
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
            echo "\u27A1 Begins here..."
            stage ('Clean workspace') {
                deleteDir()
                sh 'pwd'
                sh 'ls -lah'
            }

            stage ('Checkout source') {
                // Pull the latest commit of the specified branch(es)
                // of the specified submodule(s)
                checkout scm
                sh 'git submodule update --remote --init'
            }  
        }

        stage ('Compile') {
            sh 'gcc -Wall test-code/hello.c -o test-code/hello'
        }

        // scripted parallel
        stage ('Parallel stage') {
            parallel "integration test 1": {
                echo 'Running integration test 1'
            },
            "integration test 2": {
                echo 'Running integration test 2'
                sh 'test-code/hello'
            },
            "ui tests": {
                echo 'Running UI functional tests'
            }
        }
    }
    catch (err) {
        echo "\u27A1 Caught: ${err}"
    }

}
