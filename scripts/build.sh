rm -Rf out/*
javac -Xlint:unchecked -cp . sigPlace.java
printf "\n\n\nRunning Program...\n\n"
java sigPlace "$@"
./scripts/clean.sh