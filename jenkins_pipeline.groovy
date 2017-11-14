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

        stage ('Build') {
            echo 'Compiling software'
        }

        // scripted parallel
        stage ('Parallel stage') {
            parallel "integration test 1": {
                echo 'Running integration test 1'
            },
            "integration test 2": {
                echo 'Running integration test 2'
            },
            "ui tests": {
                echo 'Running UI functional tests'
            }
        }

/* 
        // declarative parallel?
        stage ('Parallel stage') {
            parallel {
                stage ('Run integration test 1') {
                    steps {
                        echo 'Running test 1'
                    }
                }
                stage ('Run integration test 2') {
                    steps {
                        echo 'Running test 2'
                    }
                }
                stage ('Run UI functional tests') {
                    steps {
                        echo 'Running test 3'
                    }
                }
            }
        }
*/
    }
    catch (err) {
        echo "\u27A1 Caught: ${err}"
    }

}
