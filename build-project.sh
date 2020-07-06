#! /bin/sh

chmod +x /build-project.sh
cd ..
git clone https://github.com/MinecraftHouse/Ppom.git
cd Ppom || exit
git pull
mvn install -Dgpg.skip=true -B -ntp || echo Did you install maven?
