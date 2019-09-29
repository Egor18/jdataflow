#!/usr/bin/env python3

import datetime
import sys
import re

N = 10

class Entry:
    def __init__(self, line_number, text, duration):
        self.line_number = line_number
        self.text = text
        self.duration = duration
    def __repr__(self):
        return f'{self.line_number} {self.text} ({self.duration} sec)'

def get_duration(line, next_line):
    if not line or not next_line:
        return -1
    m = re.search('^\[(.+?)\] Analyzing', line)
    m_next = re.search('^\[(.+?)\] Analyzing', next_line)
    if not m or not m_next:
        return -1
    try:
        date_time_str = m.group(1)
        date_time_str_next = m_next.group(1)
        date_time = datetime.datetime.strptime(date_time_str, '%H:%M:%S')
        date_time_next = datetime.datetime.strptime(date_time_str_next, '%H:%M:%S')
        duration = date_time_next - date_time
        return duration.total_seconds()
    except:
        return -1

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print(f'Invalid input. Usage: find-slowest.py output.txt')
        exit(1)
    output_path = sys.argv[1]
    entries = []
    with open(output_path) as f:
        lines = f.readlines()
        for i in range(len(lines) - 1):
            duration = get_duration(lines[i], lines[i+1])
            if duration != -1:
                e = Entry(i + 1, lines[i][:-1], duration)
                entries.append(e)
    entries.sort(key=lambda e: e.duration, reverse=True)
    for i in range(0, N):
        print(entries[i])
