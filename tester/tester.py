#!/usr/bin/env python3

import os
import sys
import json
import shutil
import time

projects = {

    'junit4' : {
        'download' : ['git clone https://github.com/junit-team/junit4.git junit4 --depth 1 -b r4.13-beta-3'],
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src/main/java', 'target/generated-sources/annotations'],
        'maven-generated-classpath' : True,
        'exclude' : ['target/generated-sources'],
    },

    'hikaricp' : {
        'download' : ['git clone https://github.com/brettwooldridge/HikariCP.git hikaricp --depth 1 -b HikariCP-3.3.1'],
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src/main/java', 'target/generated-sources/annotations'],
        'maven-generated-classpath' : True,
        'exclude' : ['target/generated-sources'],
    },

    'nanohttpd' : {
        'download' : ['git clone https://github.com/NanoHttpd/nanohttpd.git', 'cd nanohttpd && git checkout efb2ebf'],
        'build' : ['mvn install -DskipTests'],
        'core' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'webserver' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
    },

    'error-prone' : {
        'download' : ['git clone https://github.com/google/error-prone.git error-prone --depth 1 -b v2.3.3'],
        'build' : ['mvn compile -DskipTests'],
        'core' : {
            'sources' : ['src/main/java', 'target/generated-sources/protobuf/java', 'target/generated-sources/protobuf/grpc-java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'annotation' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'annotations' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'check_api' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'docgen' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'docgen_processor' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'refaster' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'test_helpers' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'type_annotations' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
    },

    'checkstyle' : {
        'download' : ['git clone https://github.com/checkstyle/checkstyle.git checkstyle --depth 1 -b checkstyle-8.23'],
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src/main/java', 'target/generated-sources/antlr', 'target/generated-sources/annotations'],
        'maven-generated-classpath' : True,
        'exclude' : ['target/generated-sources'],
    },

    'cactoos' : {
        'download' : ['git clone https://github.com/yegor256/cactoos.git --depth 1 -b 0.42'],
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src/main/java', 'target/generated-sources/annotations'],
        'maven-generated-classpath' : True,
        'exclude' : ['target/generated-sources'],
    },

    'takes' : {
        'download' : ['git clone https://github.com/yegor256/takes.git --depth 1 -b 1.17'],
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src/main/java', 'target/generated-sources/annotations'],
        'maven-generated-classpath' : True,
        'exclude' : ['target/generated-sources'],
    },

    'jsoup' : {
        'download' : ['git clone https://github.com/jhy/jsoup.git jsoup --depth 1 -b jsoup-1.12.1'],
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src/main/java', 'target/generated-sources/annotations'],
        'maven-generated-classpath' : True,
        'exclude' : ['target/generated-sources'],
    },

    'spark' : {
        'download' : ['git clone https://github.com/perwendel/spark.git spark --depth 1 -b 2.9.1'],
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src/main/java', 'target/generated-sources/annotations'],
        'maven-generated-classpath' : True,
        'exclude' : ['target/generated-sources'],
    },

    'algorithms' : {
        'download' : ['git clone https://github.com/TheAlgorithms/Java.git algorithms', 'cd algorithms && git checkout d6fda6f'],
        'build' : [],
        'sources' : ['.'],
    },

    'sonarqube' : {
        'download' : ['git clone https://github.com/SonarSource/sonarqube.git sonarqube --depth 1 -b 7.0'],
        'build' : ['mvn install -DskipTests -pl !":sonar-web"'],
        'sonar-core' : {
           'sources' : ['src/main/java', 'target/generated-sources/annotations'],
           'maven-generated-classpath' : True,
           'exclude' : ['target/generated-sources'],
        },
        'sonar-plugin-api' : {
           'sources' : ['src/main/java', 'target/generated-sources/annotations'],
           'maven-generated-classpath' : True,
           'exclude' : ['target/generated-sources'],
        },
        'sonar-scanner-engine' : {
           'sources' : ['src/main/java', 'target/generated-sources/annotations'],
           'maven-generated-classpath' : True,
           'exclude' : ['target/generated-sources'],
        },
    },

    'jsat' : {
        'download' : ['git clone https://github.com/EdwardRaff/JSAT.git jsat', 'cd jsat && git checkout 58671bd'],
        'root' : 'jsat/JSAT',
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src'],
        'maven-generated-classpath' : True,
    },

    'javaparser' : {
        'download' : ['git clone https://github.com/javaparser/javaparser.git javaparser --depth 1 -b v3.13.10'],
        'build' : ['mvn install -DskipTests'],
        'javaparser-core' : {
            'sources' : ['src/main/java', 'target/generated-sources/javacc', 'src/main/javacc-support', 'target/generated-sources/java-templates', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'javaparser-core-generators' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'javaparser-core-metamodel-generator' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'javaparser-core-serialization' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'javaparser-symbol-solver-core' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'javaparser-symbol-solver-logic' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'javaparser-symbol-solver-model' : {
            'sources' : ['src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        }
    },

    'guava' : {
        'download' : ['git clone https://github.com/google/guava.git guava --depth 1 -b v28.0'],
        'root' : 'guava/guava',
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src', 'target/generated-sources/annotations'],
        'maven-generated-classpath' : True,
    },

    'jenkins' : {
        'download' : ['git clone https://github.com/jenkinsci/jenkins.git jenkins --depth 1 -b jenkins-2.190'],
        'build' : ['mvn install -DskipTests'],
        'core' : {
            'sources' : ['src/main/java', 'target/generated-sources/antlr', 'target/generated-sources/localizer', 'target/generated-sources/taglib-interface', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'cli' : {
            'sources' : ['src/main/java', 'target/generated-sources/localizer', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
        'test' : {
           'sources' : ['src/test/java', 'target/generated-test-sources/test-annotations'],
           'maven-generated-classpath' : True,
           'exclude' : ['target/generated-test-sources'],
        },
    },

    'spoon' : {
        'download' : ['git clone https://github.com/INRIA/spoon.git spoon --depth 1 -b spoon-core-7.5.0'],
        'build' : ['mvn install -DskipTests -Dcheckstyle.skip'],
        'sources' : ['src/main/java'],
        'maven-generated-classpath' : True,
    },

    'gson' : {
        'download' : ['git clone https://github.com/google/gson.git gson', 'cd gson && git checkout 441fa98'],
        'build' : ['mvn install -DskipTests'],
        'gson' : {
            'sources' : ['gson/src/main/java'],
            'maven-generated-classpath' : True,
        },
        'codegen' : {
            'sources' : ['gson/src/main/java'],
            'maven-generated-classpath' : True,
        },
        'extras' : {
            'sources' : ['gson/src/main/java', 'target/generated-sources/annotations'],
            'maven-generated-classpath' : True,
            'exclude' : ['target/generated-sources'],
        },
    },

    'bukkit' : {
        'download' : ['git clone https://github.com/Bukkit/Bukkit.git bukkit --depth 1 -b 1.8.1-R4'],
        'build' : ['mvn install -DskipTests'],
        'sources' : ['src/main/java', 'src/test/java'],
        'maven-generated-classpath' : True,
    },

    'minicraft' : {
        'download' : ['git clone https://github.com/Miserlou/Minicraft.git minicraft', 'cd minicraft && git checkout 39dd1fe'],
        'build' : [],
        'sources' : ['src'],
    },
}

tester_dir = os.path.dirname(os.path.realpath(__file__))
path_to_jar = tester_dir + '/' + '../build/jdataflow.jar'
status_file = tester_dir + '/status.json'
projects_dir = tester_dir + '/projects'
outputs_dir = tester_dir + '/outputs'
reports_dir = tester_dir + '/reports'
etalons_dir = tester_dir + '/etalons'
diffs_dir = tester_dir + '/diffs'

class Project:
    def __init__(self, parent_project, project_name, project_data):
        self.parent_project = parent_project
        self.project_name = project_name
        self.project_data = project_data
        self.modules = self.get_modules()
        self.dir = self.get_dir()
        self.top_level_project = self.get_top_level_project()

    def get_modules(self):
        modules = []
        for element in self.project_data:
            if type(self.project_data[element]) is dict:
                module = Project(self, element, self.project_data[element])
                modules.append(module)
        return modules

    def get_dir(self):
        if 'root' in self.project_data:
            return projects_dir + '/' + self.project_data['root']
        else:
            if self.parent_project is not None:
                return self.parent_project.get_dir() + '/' + self.project_name
            else:
                return projects_dir + '/' + self.project_name

    def get_top_level_project(self):
        if self.parent_project is None:
            return self
        else:
            p = self.parent_project
            while p.parent_project is not None:
                p = p.parent_project
            return p

    def is_multimodule(self):
        return 'sources' not in self.project_data

    def is_top_level(self):
        return self == self.top_level_project

    def run_cmd(self, command):
        filename = self.top_level_project.project_name + '-output.txt'
        return os.system(command + f' >>{outputs_dir}/{filename} 2>>&1')

    def set_status(self, status):
        if not self.is_top_level():
            raise Exception('Should be top level project')
        try:
            stats = json.load(open(status_file, 'r'))
        except:
            stats = {self.project_name : status}
        stats[self.project_name] = status
        json.dump(stats, open(status_file, 'w'))

    def get_status(self):
        if not self.is_top_level():
            raise Exception('Should be top level project')
        try:
            stats = json.load(open(status_file, 'r'))
        except:
            return None
        if self.project_name not in stats:
            return None
        return stats[self.project_name]

    def download(self):
        if not self.is_top_level():
            raise Exception('Should be top level project')
        os.chdir(projects_dir)
        if self.get_status() != 'downloaded' and self.get_status() != 'built':
            print(f'Downloading {self.project_name}... ', end='', flush=True)
            for cmd in self.project_data['download']:
                if self.run_cmd(cmd) != 0:
                    print('FAIL')
                    exit(1)
            print('OK')
            self.set_status('downloaded')

    def build(self):
        if not self.is_top_level():
            raise Exception('Should be top level project')
        os.chdir(self.dir)
        if self.get_status() != 'built':
            print(f'Building {self.project_name}... ', end='', flush=True)
            for cmd in self.project_data['build']:
                if self.run_cmd(cmd) != 0:
                    print('FAIL')
                    exit(1)
            print('OK')
            self.set_status('built')

    def generate_classpath_file_maven(self):
        if 'maven-generated-classpath' in self.project_data and self.project_data['maven-generated-classpath']:
            if self.run_cmd('mvn dependency:build-classpath -Dmdep.outputFile=cp.txt') != 0:
                raise Exception('Unable to generate classpath file')

    def compare_report_with_etalon(self):
        project_name = self.top_level_project.project_name
        filename = project_name + '-report.txt'
        report_file = reports_dir + '/' + filename
        etalon_file = etalons_dir + '/' + filename
        diff_file = diffs_dir + '/' + filename
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

    def analyze(self):
        if self.is_top_level():
            print(f'Analyzing {self.project_name}... ', end='', flush=True)
        if self.is_multimodule():
            for module in self.modules:
                module.analyze()
        else:
            os.chdir(self.dir)
            self.generate_classpath_file_maven()
            cmd = f'java -jar {path_to_jar} --no-failsafe -r {tester_dir} -s'
            for s in self.project_data['sources']:
                cmd += ' ' + s
            if 'maven-generated-classpath' in self.project_data and self.project_data['maven-generated-classpath']:
                cmd += ' -cf cp.txt'
            elif 'classpath' in self.project_data:
                cmd += ' -cp ' + self.project_data['classpath']
            local_report_file = 'jdataflow-report.txt'
            cmd += f' -o {local_report_file}'
            if 'exclude' in self.project_data:
                cmd += ' -e'
                for e in self.project_data['exclude']:
                    cmd += ' ' + e
            if 'include' in self.project_data:
                cmd += ' -i'
                for i in self.project_data['include']:
                    cmd += ' ' + i
            if self.run_cmd(cmd) != 0:
                print('FAIL')
                exit(1)
            top_level_report_file = f'{reports_dir}/{self.top_level_project.project_name}-report.txt'
            with open(local_report_file) as local:
                with open(top_level_report_file, 'a') as top_level:
                    top_level.write(local.read())
        if self.is_top_level():
            status = self.compare_report_with_etalon()
            print(status)

def prepare_dirs():
    os.makedirs(projects_dir, exist_ok=True)
    time.sleep(0.15)
    shutil.rmtree(outputs_dir, ignore_errors=True)
    time.sleep(0.15)
    os.makedirs(outputs_dir)
    time.sleep(0.15)
    shutil.rmtree(reports_dir, ignore_errors=True)
    time.sleep(0.15)
    os.makedirs(reports_dir)
    time.sleep(0.15)
    shutil.rmtree(diffs_dir, ignore_errors=True)
    time.sleep(0.15)
    os.makedirs(diffs_dir)
    time.sleep(0.15)

def run(selected_projects):
    prepare_dirs()
    for selected_project in selected_projects:
        if selected_project not in projects:
            raise Exception('Unknown project ' + selected_project)
    for project_name in projects:
        if len(selected_projects) != 0 and project_name not in selected_projects:
            continue
        p = Project(None, project_name, projects[project_name])
        p.download()
        p.build()
        p.analyze()

if __name__ == '__main__':
    selected_projects = sys.argv[1:]
    run(selected_projects)
