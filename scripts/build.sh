#!/bin/bash

# -----------
# ----------- Prepare
# -----------

echo Insert new version
read -r version

while true; do
    echo Preparing version "$version". Is this correct [y/n]?
    read -r yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit 0;;
        * ) echo "Please answer yes or no";;
    esac
done

echo Updating ./pom.xml
sed -i '' -E "13s|.*|    <version>$version</version>|" ./pom.xml

echo Updating ./Dockerfile
sed -i '' -E "63s|.*|COPY --from=build /gover/target/Gover-$version.jar /app/gover.jar|" ./Dockerfile

echo Updating ./README
sed -i '' -E "147s|.*|This will build a JAR file at \`./target/Gover-$version.jar\`.|" ./README.md
sed -i '' -E "151s|.*|Simply run \`java -jar ./target/Gover-$version.jar\` or \`nohup java -jar ./target/Gover-$version.jar\`.|" ./README.md

echo Updating ./app/package.json
sed -i '' -E "3s|.*|  \"version\": \"$version\",|" ./app/package.json

# -----------
# ----------- Commit
# -----------

while true; do
    echo Tag and Commit [y/n]?
    read -r yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit 0;;
        * ) echo "Please answer yes or no";;
    esac
done

git add ./pom.xml
git add ./Dockerfile
git add ./README.md
git add ./app/package.json
git commit -m "Bumped version to $version"
git tag v"$version"

# -----------
# ----------- Build
# -----------

while true; do
    echo Build and push image [y/n]?
    read -r yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit 0;;
        * ) echo "Please answer yes or no";;
    esac
done

if [[ -z "${GH_USR}" ]]; then
  echo Environment variable GH_USR is not defined. Please provide a username for the github container registry!
  exit 1
fi

if [[ -z "${GH_PAT}" ]]; then
  echo Environment variable GH_PAT is not defined. Please provide a access token for the github container registry!
  exit 1
fi

docker buildx build --platform=linux/amd64 --tag ghcr.io/aivot-digital/gover:"$version" .
echo "$GH_PAT" | docker login ghcr.io -u "$GH_USR" --password-stdin
docker push ghcr.io/aivot-digital/gover:"$version"
