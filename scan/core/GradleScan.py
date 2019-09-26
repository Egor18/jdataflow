#!/usr/bin/env python3

import os
from core.AbstractScan import AbstractScan

class GradleScan(AbstractScan):
    def get_debug_flag(self):
        return '--debug'

    def remove_non_existent(self, classpath):
        splitter = ';' if os.name == 'nt' else ':'
        elements = classpath.split(splitter)
        result = ''
        for element in elements:
            if os.path.exists(element):
                result += element + splitter
        return result

    def scan_build(self, build_output):
        compiler_args_start_pattern = 'Compiler arguments:'
        modules = []
        for line in build_output:
            if compiler_args_start_pattern in line:
                compiler_args = line.split()
                classpath = None
                if '-classpath' in compiler_args:
                    i = compiler_args.index('-classpath')
                    classpath = compiler_args[i + 1]
                    classpath = self.remove_non_existent(classpath)
                sources = []
                for arg in reversed(compiler_args):
                    if not arg.endswith('.java'):
                        break
                    sources.append(arg)
                if sources:
                    modules.append((sources, classpath))
        return modules
