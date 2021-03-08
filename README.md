
Snowdrop is an HTTP implementation of [Twitter's Snowflake] (https://github.com/twitter/snowflake).

### Usage

```
git clone https://github.com/buom/snowdrop.git
cd snowdrop
./gradlew build
tar xf app/build/distributions/snowdrop-1.0.tar
export APP_OPTS=" -DworkerId=0 -DbossThreads=1 -DworkerThreads=4"
./snowdrop-1.0/bin/snowdrop
```