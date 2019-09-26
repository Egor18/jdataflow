#!/usr/bin/env python3

import os
import sys
import json
import argparse
import tempfile
import subprocess

class AbstractScan:
    def __init__(self):
        self.script_dir = os.path.dirname(os.path.realpath(__file__))
        self.cache_dir = '.jdataflow'
        self.modules_cache_file = self.cache_dir + '/' + 'modules-cache'
        self.path_to_jar = self.get_path_to_jar()

    def get_path_to_jar(self):
        possible_paths = [self.script_dir,
                          self.script_dir + '/' + '../',
                          self.script_dir + '/' + '../' + '../',
                          self.script_dir + '/' + '../build',
                          self.script_dir + '/' + '../' + '../build']
        for p in possible_paths:
            jar_path = p + '/' + 'jdataflow.jar'
            if os.path.exists(jar_path):
                return jar_path
        raise Exception('Unable to find jdataflow.jar')

    def make_args_parser(self):
        name = os.path.basename(sys.argv[0])
        parser = argparse.ArgumentParser(usage=f'{name} [options] -- <build command> [build command options]', add_help=False)
        parser.add_argument('-o', '--output', help='path to the file to output the report')
        parser.add_argument('-e', '--excludes', nargs='*', help='exclude these files/dirs from analysis')
        parser.add_argument('-i', '--includes', nargs='*', help='analyze only these files/dirs')
        parser.add_argument('-r', '--relativizer', help='relativize paths in report against this path')
        parser.add_argument('--show-build-output', action='store_true', help='show build output in the console')
        parser.add_argument('--no-failsafe', action='store_true', help='terminate analysis immediately on any internal error')
        parser.add_argument('--use-modules-cache', action='store_true', help=argparse.SUPPRESS) # Functionality for the testing system
        return parser

    def split_argv(self):
        split_index = sys.argv.index('--')
        arguments = sys.argv[:split_index]
        command = sys.argv[split_index + 1:]
        return (arguments, command)

    def run_build_command(self, command_str, show_build_output):
        if not show_build_output:
            print('Running build command... ', end='', flush=True)
        proc = subprocess.Popen(command_str, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True, shell=True)
        lines = []
        for line in proc.stdout:
            if show_build_output:
                sys.stdout.write(line)
            lines.append(line)
        rc = proc.wait()
        if not show_build_output:
            print('OK' if rc == 0 else 'FAIL', flush=True)
        return (rc, lines)

    def check_modules(self, modules, args):
        i = 0
        if args.output:
            output_path = args.output
            print_to_stdout = False
            if os.path.exists(output_path):
                os.remove(output_path)
        else:
            output_fd, output_path = tempfile.mkstemp()
            print_to_stdout = True
        for (sources, classpath) in modules:
            current_output_fd, current_output_path = tempfile.mkstemp()
            config = {}
            config['sources'] = sources
            if classpath:
                config['classpath'] = classpath
            if current_output_path:
                config['output'] = current_output_path
            if args.excludes:
                config['excludes'] = args.excludes
            if args.includes:
                config['includes'] = args.includes
            if args.relativizer:
                config['relativizer'] = args.relativizer
            if args.no_failsafe:
                config['no-failsafe'] = args.no_failsafe
            with open(current_output_path, 'w') as current_output_file:
                fd, path = tempfile.mkstemp()
                with open(path, 'w') as config_file:
                    json.dump(config, config_file, indent=4)
                rc = os.system(f'java -jar {self.path_to_jar} --config-file {path}')
                os.close(fd)
                os.remove(path)
                if rc != 0:
                    exit(1)
            with open(output_path, 'a') as output_file:
                with open(current_output_path, 'r') as current_output_file:
                    output_file.write(current_output_file.read())
            os.close(current_output_fd)
            os.remove(current_output_path)
            i += 1
        if print_to_stdout:
            with open(output_path, 'r') as output_file:
                print(output_file.read(), flush=True)
            os.close(output_fd)
            os.remove(output_path)

    def run(self):
        parser = self.make_args_parser()
        if '-h' in sys.argv or '--help' in sys.argv:
            parser.print_help()
            exit(0)
        if '--' not in sys.argv:
            parser.print_help()
            exit(1)
        arguments, command = self.split_argv()
        args = parser.parse_args(arguments[1:])
        command.append(self.get_debug_flag())
        command_str = ' '.join(command)
        if args.use_modules_cache:
            if not os.path.exists(self.cache_dir):
                os.mkdir(self.cache_dir)
            if os.path.exists(self.modules_cache_file):
                print('Cache was found', flush=True)
                with open(self.modules_cache_file, 'r') as f:
                    modules = json.load(f)
            else:
                print('Cache was not found', flush=True)
                rc, build_output = self.run_build_command(command_str, args.show_build_output)
                if rc != 0:
                    exit(1)
                modules = self.scan_build(build_output)
                with open(self.modules_cache_file, 'w') as f:
                    json.dump(modules, f, indent=4)
        else:
            rc, build_output = self.run_build_command(command_str, args.show_build_output)
            if rc != 0:
                exit(1)
            modules = self.scan_build(build_output)
        if not modules:
            print('No sources were found', flush=True)
            exit(1)
        self.check_modules(modules, args)

    def get_debug_flag(self):
        raise NotImplementedError('Subclasses must override get_debug_flag!')

    def scan_build(self, build_output):
        raise NotImplementedError('Subclasses must override scan_build!')
