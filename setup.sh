#!/bin/bash

PROJECT_ROOT=$(pwd)

if [[ ! -f testing.properties ]] || [[ ! -f production.properties ]];
then
	echo -e "\e[1;31mPlease obtain the \e[1;32mtesting.properties\e[1;31m and \e[1;32mproduction.properties\e[1;31m files and place them in the project root ($PROJECT_ROOT)\e[0m"
	exit 1
fi

if [[ ! `which aws 2> /dev/null` ]];
then
	echo -e "\e[1;31mPlease configure awscli (pip install awscli, optionally in a virtualenv) so it's in the path (as aws) and rerun the script, skipping DynamoDB setup.\e[0m"
	exit 1
fi

# Fetch DynamoDB if needed
while [[ ! "$SET_UP_DYNAMO" =~ ^[Yy]$ ]] && [[ ! "$SET_UP_DYNAMO" =~ ^[Nn]$ ]]
do
	read -p "Download and start dynamo? [y/n] " -n 1 SET_UP_DYNAMO
	echo
done

if [[ "$SET_UP_DYNAMO" =~ ^[Yy]$ ]]
then
	mkdir -p "$PROJECT_ROOT/db"
	cd "$PROJECT_ROOT/db"

	echo "Retrieving latest DynamoDB."
	wget -q "https://s3.eu-central-1.amazonaws.com/dynamodb-local-frankfurt/dynamodb_local_latest.tar.gz"

	echo "Downloading and checking hash."
	wget -q "https://s3.eu-central-1.amazonaws.com/dynamodb-local-frankfurt/dynamodb_local_latest.tar.gz.sha256"
	
	if sha256sum --quiet -c dynamodb_local_latest.tar.gz.sha256
	then
		echo "Hash matched, setting up db directory and extracting db."
		tar xaf dynamodb_local_latest.tar.gz

		echo -e "\nPlease read the attached licenses carefully."
		sleep 2
		echo "Thank you for reading the licenses carefully. Tidying up the db directory."

		rm -r dynamodb_local_latest.tar.gz dynamodb_local_latest.tar.gz.sha256 third_party_licenses LICENSE.txt README.txt

		echo -e "\n\e[1;31mPlease ensure port 8765 is not in use, press enter to continue when ready to continue.\e[0m"
		read
		echo "Starting dynamo in the background."
		java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -port 8765 > /dev/null &
		echo "Stop Dynamo with 'kill $!' if needed."

	else
		echo -e "\e[1;31mHash did not match. Cleaning up DynamoDB downloads.\e[0m"
		rm dynamodb_local_latest.tar.gz dynamodb_local_latest.tar.gz.sha256
	   	echo -e "\e[1;31mRefusing to set up dynamodb, will continue other set up.\e[0m"
	fi
	# return to project dir as expected outside the if
	cd "$PROJECT_ROOT"
else
	echo -e "\e[1;31mPlease start Dynamo on port 8765 before continuing. Hit enter when ready.\e[0m"
	read
fi

echo "Setting up awscli profile and creating tables."

aws configure --profile dario set aws_access_key_id blah
aws configure --profile dario set aws_secret_access_key blah

aws dynamodb create-table --profile dario --endpoint-url http://localhost:8765 --cli-input-json file://db/login.json > /dev/null
aws dynamodb create-table --profile dario --endpoint-url http://localhost:8765 --cli-input-json file://db/users.json > /dev/null

echo -e "\nSetting up config files"

mv testing.properties "$PROJECT_ROOT/code/src/test/resources/config.properties"
mkdir -p "$PROJECT_ROOT/code/src/main/resources"
mv production.properties "$PROJECT_ROOT/code/src/main/resources/config.properties"

echo -en "\nProject set up. "
echo 'Change to the code directory and run `mvn test` to see the tests working, or `mvn package` to produce a runnable jar (which will run the tests anyway).'
