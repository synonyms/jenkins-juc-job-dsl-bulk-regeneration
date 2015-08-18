import org.kohsuke.github.GitHub

def password = "b24e03bfa50154730587ccab59929dbb934e8078"
println password
def gh = GitHub.connect("rhels", password)
gh.getOrganization("ci-cd").listRepositories().each { repo ->
	println(repo.fullName)
	job(repo.fullName.replace('//', '-')){
		description(repo.getDescription() )
		logRotator(2, 10, -1, -1)
		jdk("jdk7")
		label('linux')
		//scm{ git("${gitCloneURL}")}
		triggers{
			scm('@hourly')
			githubPush()
			//wipeOutWorkspace(true)
		}
		disabled(true)
		quietPeriod()
		wrappers {
			maskPasswords()
			buildUserVars()
			preBuildCleanup()
			logSizeChecker {
				maxSize(10) // using job specific configuration, setting the max log size to 10 MB and fail the build of the log file is larger.
				failBuild()
			}
			timeout {
				elastic(300, // Build will timeout when it take 3 time longer than the reference build duration, default = 150
						3,   // Number of builds to consider for average calculation
						60   // 30 minutes default timeout (no successful builds available as reference)
						)
				writeDescription('Build failed due to timeout')
			}
			timestamps()
			colorizeOutput() //AnsiColor Plugin.
		}


		scm {
			git {
				remote {
					github(repo.full_name, 'ssh', 'github.discoverfinancial.com')
					credentials('a30d8231-1c4e-4085-aade-3a5ac3bc3bfa')
					branch('refs/heads/master')
				}
			}
			//clean()
		}
		steps {
			gradle('check')
			gradle {
				makeExecutable(true)
				description("run the gradle assemble cmd")
				tasks('clean')
				tasks('assemble')
				tasks('uploadArchives')
				tasks('cfPush')
				tasks('check')
				switches('-PnexusPublicRepoURL=\${nexusPublicRepoURL}')
				switches('-PnexusReleaseRepoURL=\${nexusReleaseRepoURL}')
				switches('-PnexusSnapshotRepoURL=\${nexusSnapshotRepoURL}')
				switches('-PnexusUsername=\${nexusUsername}')
				switches('-PnexusPassword=\${nexusPassword}')
				switches('-PcfUsername=\${cfUsername}')
				switches('-PcfPassword=\${cfPassword}')
				switches('-Pclassifier=BUILD')
				switches('-PbuildNumber=\${BUILD_NUMBER}')
				switches('-Pdev')
				switches('--info')
			}
		}
		publishers {
			archiveJunit 'build/test-results/**/*.xml'
			githubCommitNotifier()
			chucknorris()
		}


	}
}