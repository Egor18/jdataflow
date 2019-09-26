#!/usr/bin/env python3

import os
from core.AbstractScan import AbstractScan

class MavenScan(AbstractScan):
    def get_debug_flag(self):
        return '-X -B'

    def scan_build(self, build_output):
        modules = []
        classpath_start_pattern = '[DEBUG] Classpath:'
        sources_start_pattern = '[DEBUG] Source roots:'
        splitter = ';' if os.name == 'nt' else ':'
        classpath = ''
        sources = []
        inside_classpath, inside_sources = False, False
        for line in build_output:
            line = line.strip()
            if line == classpath_start_pattern:
                inside_classpath, inside_sources = True, False
                continue
            if line == sources_start_pattern:
                inside_classpath, inside_sources = False, True
                continue
            if inside_classpath:
                path = line.split('[DEBUG]  ')[1].strip()
                if path and os.path.exists(path):
                    classpath += path + splitter
            if inside_sources:
                if line.startswith('[DEBUG]  '):
                    path = line.split('[DEBUG]  ')[1].strip()
                    if path and os.path.exists(path):
                        sources.append(path)
                else:
                    if sources:
                        modules.append((sources, classpath))
                    inside_classpath, inside_sources = False, False
                    classpath = ''
                    sources = []
        return modules
