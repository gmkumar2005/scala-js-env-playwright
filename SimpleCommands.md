### Release process

#### Verify gpg is working

```sh
echo "test" | gpg --clearsign -u F7E440260BAE93EB4AD2723D6613CA76E011F638
```

```text
export SONATYPE_USERNAME=<<username>>
export SONATYPE_PASSWORD=<<password>>
```

#### And in your credentials file (~/.sbt/1.0/sonatype.sbt):

```
credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "s01.oss.sonatype.org",  // Note the s01 subdomain
  "your-username",
  "your-password"
)
```

#### Copy sonatype.sbt to sonatype.credentials 

```
cp sonatype.sbt sonatype.credentials
```
### Run sbt commands
```sh
sbt release 
*** Current release and next release has to be same. 

sbt sonatypeBundleRelease

```

### Verify
```
Login to central.sonatype.com
Check https://central.sonatype.com/publishing/deployments

Wait for 24 hrs for publishing to complete

``` 