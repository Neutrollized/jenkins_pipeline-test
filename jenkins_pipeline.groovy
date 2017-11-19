//@Library('github.com/Neutrollized/shared-jenkins-library@master')

//vars
if (!env.ANGULARCLI_IMAGE}) {env.ANGULARCLI_IMAGE = 'neutrollized/chromium-headless-ng'}
if (!env.ANGULARCLI_VER) {env.ANGULARCLI_VER = '1.1.0'}
// Mandatory Jenkinsfile vars
//if (!env.BUILD_REPO) {error 'BUILD_REPO must be defined in Jenkinsfile environment.'}

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

	docker.image("${env.ANGULARCLI_IMAGE}:${env.ANGULARCLI_VER}").inside('--privileged -p 9876:9876 -p 49152:49152') {
	    stage ('Compiling project within docker container') {
	    	sh 'cd test-code/angular-realworld-example-app && npm install && ng build'
	    }
	    stage ('Karma Unit test') {
		sh 'npm install karma'
		sh 'cd test-code/angular-realworld-example-app && ng test --watch=false --browsers ChromeHeadless'
            }
/*   no need to start ng serve for ng e2e
	    stage ('Start ng serve') {
		sh 'cd test-code/angular-realworld-example-app && ng serve --host 0.0.0.0 --disable-host-check &'
            }
*/
            stage ('Parallel testing within docker container') {
            	parallel "Protractor E2E test": {
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
