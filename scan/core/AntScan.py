#!/usr/bin/env python3

import os
from core.AbstractScan import AbstractScan

class AntScan(AbstractScan):
    def get_debug_flag(self):
        return '-verbose'

    def scan_build(self, build_output):
        modules = []
        classpath_start_pattern = "[javac] '-classpath'"
        sourcepath_start_pattern = "[javac] '-sourcepath'"
        splitter = ';' if os.name == 'nt' else ':'
        classpath = ''
        sources = []
        for i in range(len(build_output)):
            if build_output[i].strip() == classpath_start_pattern:
                classpath = build_output[i + 1].split("'")[1]
            if build_output[i].strip() == sourcepath_start_pattern:
                sourcepath = build_output[i + 1].split("'")[1]
                sources = sourcepath.split(splitter)
            if sources and classpath:
                modules.append((sources, classpath))
                classpath = ''
                sources = []
        return modules
