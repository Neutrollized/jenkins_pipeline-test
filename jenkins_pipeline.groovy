node('docker') {
    try {
        // requires AnsiColor plugin
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
            echo "\u27A1 Begins here..."
            stage ('Clean workspace') {
                deleteDir()
                sh 'ls -lah'
            }

            stage ('Checkout source') {
                // Pull the latest commit of the specified branch(es)
                // of the specified submodule(s)
                checkout scm
                sh 'git submodule update --remote --init'
            }  
        }

        stage ('Compile C') {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
                sh 'gcc -Wall test-code/hello.c -o test-code/hello'
            }
        }

        stage ('Compile Go') {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
                sh 'go build -o ./test-code/hello-world ./test-code/hello-world.go'
            }
        }

	docker.image('neutrollized/ng:1.1.0').inside('--net=host') {
	    stage ('Compiling project within docker container') {
	    	sh 'cd test-code/angular-realworld-example-app && npm install && ng build'
	    }
/*
	    stage ('Unit test') {
                sh 'npm install karma'
		sh 'cd test-code/angular-realworld-example-app && ng test'
            }
*/
	    stage ('Start ng serve') {
		sh 'cd test-code/angular-realworld-example-app && ng serve --host 0.0.0.0 --disable-host-check &'
            }
            stage ('Parallel testing within docker container') {
            	parallel "docker e2e test 1": {
//                    sh 'npm install protractor'
                    sh 'cd test-code/angular-realworld-example-app && npm install protractor && ng e2e'
            	},
                "docker test 2": {
                    sh 'date'
            	},
            	"docker test 3": {
		    sh 'hostname'
            	}
            }
	}

/*
        stage ('Compile Angular') {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
		// change to subdirectory and execute commands within it
                dir ('test-code/angular-realworld-example-app/') {
		    sh 'npm install'
                   sh 'ng build'
		}
            }
        }

        stage ('Unit tests') {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
                dir ('test-code/angular-realworld-example-app/') {
  	    	    sh 'npm install karma'
	    	    sh 'export CHROME_BIN=/usr/bin/chromium-browser'
                    sh 'ng test'
		}
            }
        }
*/

        // scripted parallel
        stage ('Parallel testing stage') {
            parallel "integration test 1": {
                sh 'test-code/hello'
            },
            "integration test 2": {
                sh 'test-code/hello-world'
            },
            "ui tests": {
		sh 'echo stuff here'
            }
        }
    }
    catch (err) {
        echo "\u27A1 Caught: ${err}"
    }

}
