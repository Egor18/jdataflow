#!/usr/bin/env python3

import os
import sys
import json
import shutil
import time

PROJECTS = {
    'jdataflow' : { # eat your own dog food, right?
        'download' : ['git clone https://github.com/Egor18/jdataflow.git jdataflow', 'cd jdataflow && git checkout 17eb683'],
        'build-system' : 'gradle',
        'build' : ['./gradlew clean assemble'],
    },

    'spring-framework' : {
        'download' : ['git clone https://github.com/spring-projects/spring-framework.git spring-framework --depth 1 -b v5.2.0.RC2'],
        'build-system' : 'gradle',
        'build' : ['./gradlew clean assemble'],
        'exclude' : ['spring-core/src/main/java/org/springframework/asm'], # takes too long
    },

    'terasology' : {
        'download' : ['git clone https://github.com/MovingBlocks/Terasology.git terasology --depth 1 -b v2.2.0'],
        'build-system' : 'gradle',
        'build' : ['./gradlew clean assemble'],
    },

    'spotbugs' : {
        'download' : ['git clone https://github.com/spotbugs/spotbugs.git spotbugs --depth 1 -b 4.0.0_beta3'],
        'build-system' : 'gradle',
        'build' : ['./gradlew clean assemble'],
        'exclude' : ['spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindPuzzlers.java'], # takes too long
    },

    'kafka' : {
        'download' : ['git clone https://github.com/apache/kafka.git kafka --depth 1 -b 2.4.0-rc0', 'cd kafka && gradle'],
        'build-system' : 'gradle',
        'build' : ['./gradlew clean assemble'],
        'exclude' : ['clients/src/generated'],
    },

    'mockito' : {
        'download' : ['git clone https://github.com/mockito/mockito.git mockito --depth 1 -b v3.1.10'],
        'build-system' : 'gradle',
        'build' : ['./gradlew clean assemble'],
    },

    'tomcat' : {
        'download' : ['git clone https://github.com/apache/tomcat.git tomcat --depth 1 -b 9.0.26'],
        'build-system' : 'ant',
        'build' : ['ant clean compile'],
        'exclude' : [
            'java/org/apache/coyote/http2/HPackHuffman.java',
            'java/org/apache/el/parser',
            'java/org/apache/tomcat/util/http/parser/HttpParser.java', # takes too long
            'java/org/apache/tomcat/util/descriptor/web/WebXml.java', # takes too long
            'java/org/apache/tomcat/util/json',
        ],
    },

    'arduino' : {
        'download' : ['git clone https://github.com/arduino/Arduino.git arduino --depth 1 -b 1.8.10'],
        'root' : 'arduino/arduino-core',
        'build-system' : 'ant',
        'build' : ['ant clean compile'],
    },

    'junit4' : {
        'download' : ['git clone https://github.com/junit-team/junit4.git junit4 --depth 1 -b r4.13-beta-3'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'hikaricp' : {
        'download' : ['git clone https://github.com/brettwooldridge/HikariCP.git hikaricp --depth 1 -b HikariCP-3.3.1'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'nanohttpd' : {
        'download' : ['git clone https://github.com/NanoHttpd/nanohttpd.git', 'cd nanohttpd && git checkout efb2ebf'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'error-prone' : {
        'download' : ['git clone https://github.com/google/error-prone.git error-prone --depth 1 -b v2.3.3'],
        'build-system' : 'maven',
        'build' : ['mvn clean compile -DskipTests'],
    },

    'checkstyle' : {
        'download' : ['git clone https://github.com/checkstyle/checkstyle.git checkstyle --depth 1 -b checkstyle-8.23'],
        'build-system' : 'maven',
        'build' : ['mvn clean compile -DskipTests'],
        'exclude' : ['target/generated-sources', 'target/generated-test-sources'],
    },

    'cactoos' : {
        'download' : ['git clone https://github.com/yegor256/cactoos.git --depth 1 -b 0.42'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'takes' : {
        'download' : ['git clone https://github.com/yegor256/takes.git --depth 1 -b 1.17'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'jsoup' : {
        'download' : ['git clone https://github.com/jhy/jsoup.git jsoup --depth 1 -b jsoup-1.12.1'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'spark' : {
        'download' : ['git clone https://github.com/perwendel/spark.git spark --depth 1 -b 2.9.1'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'sonarqube' : {
        'download' : ['git clone https://github.com/SonarSource/sonarqube.git sonarqube --depth 1 -b 7.0'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests -pl !":sonar-web"'],
        'exclude' : [
            'sonar-ws/target/generated-sources',
            'sonar-ws/target/generated-test-sources',
            'sonar-scanner-protocol/target/generated-sources',
            'sonar-scanner-protocol/target/generated-test-sources',
            'server/sonar-db-dao/target/generated-sources',
            'server/sonar-db-dao/target/generated-test-sources',
            'server/sonar-process/target/generated-sources',
            'server/sonar-process/target/generated-test-sources',
        ],
    },

    'jsat' : {
        'download' : ['git clone https://github.com/EdwardRaff/JSAT.git jsat', 'cd jsat && git checkout 58671bd'],
        'root' : 'jsat/JSAT',
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'javaparser' : {
        'download' : ['git clone -c core.longpaths=true https://github.com/javaparser/javaparser.git javaparser --depth 1 -b v3.13.10'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
        'exclude' : ['javaparser-core/target/generated-sources', 'javaparser-core/src/main/java/com/github/javaparser/metamodel/JavaParserMetaModel.java'],
    },

    'guava' : {
        'download' : ['git clone https://github.com/google/guava.git guava --depth 1 -b v28.0'],
        'root' : 'guava/guava',
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'jenkins' : {
        'download' : ['git clone https://github.com/jenkinsci/jenkins.git jenkins --depth 1 -b jenkins-2.190'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
        'exclude' : ['core/target/generated-sources', 'cli/target/generated-sources'],
    },

    'spoon' : {
        'download' : ['git clone https://github.com/INRIA/spoon.git spoon --depth 1 -b spoon-core-7.5.0'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests -Dcheckstyle.skip'],
        'exclude' : ['src/test/java/spoon/test/api/Metamodel.java'], # takes too long
    },

    'gson' : {
        'download' : ['git clone https://github.com/google/gson.git gson', 'cd gson && git checkout 441fa98'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'bukkit' : {
        'download' : ['git clone https://github.com/Bukkit/Bukkit.git bukkit --depth 1 -b 1.8.1-R4'],
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
    },

    'h2' : {
        'download' : ['git clone https://github.com/h2database/h2database.git h2 --depth 1 -b version-1.4.200'],
        'root' : 'h2/h2',
        'build-system' : 'maven',
        'build' : ['mvn clean install -DskipTests'],
        'exclude' : ['src/main/org/h2/command/Parser.java'], # takes too long
    },

    'algorithms' : {
        'download' : ['git clone https://github.com/TheAlgorithms/Java.git algorithms', 'cd algorithms && git checkout d6fda6f'],
        'build-system' : None,
        'build' : [],
        'sources' : ['.'],
    },

    'minicraft' : {
        'download' : ['git clone https://github.com/Miserlou/Minicraft.git minicraft', 'cd minicraft && git checkout 39dd1fe'],
        'build-system' : None,
        'build' : [],
        'sources' : ['src'],
    },
}

TESTER_DIR = os.path.dirname(os.path.realpath(__file__))
PATH_TO_JAR = TESTER_DIR + '/../build/jdataflow.jar'
SCANNERS_DIR = TESTER_DIR + '/../scan'
PROJECTS_DIR = TESTER_DIR + '/projects'
OUTPUTS_DIR = TESTER_DIR + '/outputs'
REPORTS_DIR = TESTER_DIR + '/reports'
ETALONS_DIR = TESTER_DIR + '/etalons'
DIFFS_DIR = TESTER_DIR + '/diffs'

class Project:
    def __init__(self, project_name, project_data):
        self.project_name = project_name
        self.project_data = project_data
        self.dir = self.get_project_dir()

    def get_project_dir(self):
        if 'root' in self.project_data:
            return PROJECTS_DIR + '/' + self.project_data['root']
        else:
            return PROJECTS_DIR + '/' + self.project_name

    def run_cmd(self, command):
        if os.name == 'nt' and command.startswith('./'):
            command = command[2:]
        filename = self.project_name + '-output.txt'
        return os.system(command + f' >>{OUTPUTS_DIR}/{filename} 2>&1')

    def download(self):
        os.chdir(PROJECTS_DIR)
        if not os.path.exists(self.dir):
            print(f'Downloading {self.project_name}... ', end='', flush=True)
            for cmd in self.project_data['download']:
                if self.run_cmd(cmd) != 0:
                    print('FAIL')
                    exit(1)
            print('OK')

    def compare_report_with_etalon(self):
        project_name = self.project_name
        filename = project_name + '-report.txt'
        report_file = REPORTS_DIR + '/' + filename
        etalon_file = ETALONS_DIR + '/' + filename
        diff_file = DIFFS_DIR + '/' + filename
        if not os.path.exists(etalon_file):
            return 'NO ETALON'
        with open(report_file) as r:
            lines = r.readlines()
            report_lines = [line.strip() for line in lines]
        with open(etalon_file) as e:
            lines = e.readlines()
            etalon_lines = [line.strip() for line in lines]
        missing = []
        additional = []
        for line in etalon_lines:
            if line not in report_lines:
                missing.append(line)
        for line in report_lines:
            if line not in etalon_lines:
                additional.append(line)
        if missing or additional:
            with open(diff_file, 'w') as d:
                if missing:
                    d.write('----- MISSING WARNINGS -----\n')
                    for m in missing:
                        d.write(m + '\n')
                if missing and additional:
                    d.write('\n')
                if additional:
                    d.write('----- ADDITIONAL WARNINGS -----\n')
                    for a in additional:
                        d.write(a + '\n')
            return 'DIFF'
        else:
            return 'OK'

    def make_parameters(self):
        cmd = ''
        report_path = f'{REPORTS_DIR}/{self.project_name}-report.txt'
        if 'sources' in self.project_data:
            cmd += ' -s'
            for s in self.project_data['sources']:
                cmd += ' ' + s
        if 'classpath' in self.project_data:
            cmd += ' -cp ' + self.project_data['classpath']
        if 'classpath-file' in self.project_data:
            cmd += ' -cf ' + self.project_data['classpath-file']
        cmd += f' -r {TESTER_DIR}'
        cmd += f' -o {report_path}'
        if 'exclude' in self.project_data:
            cmd += ' -e'
            for e in self.project_data['exclude']:
                cmd += ' ' + e
        if 'include' in self.project_data:
            cmd += ' -i'
            for i in self.project_data['include']:
                cmd += ' ' + i
        return cmd

    def check(self):
        os.chdir(self.dir)
        print(f'Analyzing {self.project_name}... ', end='', flush=True)
        if 'build-system' not in self.project_data or not self.project_data['build-system']:
            if 'sources' not in self.project_data:
                raise Exception('No source files')
            if 'build' in self.project_data:
                for cmd in self.project_data['build']:
                    if self.run_cmd(cmd) != 0:
                        print('FAIL')
                        exit(1)
            cmd = f'java -jar {PATH_TO_JAR} --no-failsafe'
            cmd += self.make_parameters()
            start_time = time.time()
            if self.run_cmd(cmd) != 0:
                print('FAIL')
                exit(1)
        elif self.project_data['build-system'] == 'maven' or \
             self.project_data['build-system'] == 'gradle' or \
             self.project_data['build-system'] == 'ant':
                if len(self.project_data['build']) != 1:
                    raise Exception('Invalid build command')
                build_cmd = self.project_data['build'][0]
                if os.name == 'nt' and build_cmd.startswith('./'):
                    build_cmd = build_cmd[2:]
                build_system_name = self.project_data['build-system']
                scanner_path = SCANNERS_DIR + '/' + f'scan-{build_system_name}.py'
                report_path = f'{REPORTS_DIR}/{self.project_name}-report.txt'
                cmd = f'{scanner_path} --no-failsafe --use-modules-cache --show-build-output'
                cmd += self.make_parameters()
                cmd +=  f' -- {build_cmd}'
                start_time = time.time()
                if self.run_cmd(cmd) != 0:
                    print('FAIL')
                    exit(1)
        else:
            raise Exception('Unknown build system')
        elapsed_time = time.time() - start_time
        duration = time.strftime('(%Hh:%Mm:%Ss)', time.gmtime(elapsed_time))
        status = self.compare_report_with_etalon()
        print(status + ' ' + duration)

def prepare_dirs():
    os.makedirs(PROJECTS_DIR, exist_ok=True)
    time.sleep(0.15)
    shutil.rmtree(OUTPUTS_DIR, ignore_errors=True)
    time.sleep(0.15)
    os.makedirs(OUTPUTS_DIR)
    time.sleep(0.15)
    shutil.rmtree(REPORTS_DIR, ignore_errors=True)
    time.sleep(0.15)
    os.makedirs(REPORTS_DIR)
    time.sleep(0.15)
    shutil.rmtree(DIFFS_DIR, ignore_errors=True)
    time.sleep(0.15)
    os.makedirs(DIFFS_DIR)
    time.sleep(0.15)

def run(selected_projects):
    prepare_dirs()
    for selected_project in selected_projects:
        if selected_project not in PROJECTS:
            raise Exception('Unknown project ' + selected_project)
    for project_name in PROJECTS:
        if len(selected_projects) != 0 and project_name not in selected_projects:
            continue
        p = Project(project_name, PROJECTS[project_name])
        p.download()
        p.check()

if __name__ == '__main__':
    total_start_time = time.time()
    selected_projects = sys.argv[1:]
    run(selected_projects)
    total_elapsed_time = time.time() - total_start_time
    total_duration = time.strftime('(%Hh:%Mm:%Ss)', time.gmtime(total_elapsed_time))
    print('Total time: ' + total_duration)
