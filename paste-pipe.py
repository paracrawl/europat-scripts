#!/usr/bin/env python3
import sys
import subprocess
from threading import Thread
from queue import Queue


def stdin_to_child(out_queue, stdin, child_in):
    try:
        for line in stdin:
            line = line.rstrip("\n")
            child_in.write(line + "\n")
            out_queue.put(line)

        out_queue.put(None)
    except Exception as e:
        out_queue.put(e)
    finally:
        child_in.close()


def child_to_stdout(out_queue, child_out, stdout):
    while True:
        line = out_queue.get()

        # None is poison
        if line is None:
            break

        if isinstance(line, Exception):
            raise line

        child_line = child_out.readline().rstrip("\n")
        stdout.write(line + "\t" + child_line + "\n")


def usage(prog):
    print(f"Usage: {prog} command [arg...]", file=sys.stderr)
    return 1


def main(argv):
    if len(argv) <= 1:
        return usage(argv[0])

    child = subprocess.Popen(argv[1:], stdin=subprocess.PIPE, stdout=subprocess.PIPE, encoding='utf-8')

    queue = Queue()

    feeder = Thread(target=stdin_to_child, args=[queue, sys.stdin, child.stdin], daemon=True)
    feeder.start()

    child_to_stdout(queue, child.stdout, sys.stdout)

    feeder.join()

    child.wait()

    return child.returncode


sys.exit(main(sys.argv))
