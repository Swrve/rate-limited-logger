
## How to push a new release

## Step 0: check the version numbers!

Check README.md and build.gradle; the correct version number should
crop up in several locations.


## Step 1: regenerate javadocs

Run "release/publish-javadoc" to regenerate the javadoc website.
Note: this is ok to run at any time before release, assuming the
release number in build.gradle has already been updated to point
at the next release number.


## Step 2: Uploading new signed JARs

Get hold of the release-signing secrets.

Run:

  cp release/signing/gradle.properties ~/.gradle/gradle.properties
  ./gradlew uploadArchives

Go to https://oss.sonatype.org/#stagingRepositories and log in using
the credentials in release/signing/gradle.properties .

Scroll to the bottom of the list looking for a task starting with
"comswrve".  Tick it then hit the "Close" button and enter a brief
message indicating that it's a release.

After a few seconds, you should be able to then hit the "Release" button.

Warning: do NOT check in the "signing" dir!  It contains secret key data.


## Step 3: Tag

    git tag -a release-N.N.N -m 'new release'
    git push origin release-N.N.N


## See Also / Links

http://zserge.com/blog/gradle-maven-publish.html
