import re
import numpy as np
import pandas as pd
from pathlib import Path
import matplotlib.pyplot as plt

def openCsv(path):
    regex = re.compile('\d')
    with open(path, 'r') as file:
        lines = filter(lambda x: regex.match(x[0]), file.readlines())
        return [[float(x) for x in line.split()] for line in lines]

def extractVariableNames(filename):
    with open(filename, 'r') as file:
        dataBegin = re.compile('\d')
        lastHeaderLine = ''
        for line in file:
            if dataBegin.match(line[0]):
                break
            else:
                lastHeaderLine = line
        if lastHeaderLine:
            regex = re.compile(' (?P<varName>\S+)')
            return regex.findall(lastHeaderLine)
        return []

def generate_charts(name = ''):
    csv_file = 'data/track-movement-distributed/track-movement-distributed_numberOfParticles-250_maxInitialSpeed-2.0.csv'
    # csv_file = 'data/experiment.csv'

    lines = np.matrix(openCsv(csv_file))
    vars =  extractVariableNames(csv_file)
    vars = [v.split('[')[0] for v in vars]
    df = pd.DataFrame(data=lines, columns=vars)
    df = df.dropna()

    side_length = 100

    df_estimation = pd.read_csv(f'data/estimations{name}.csv')

    plt.figure(figsize=(10, 10))

    plt.plot(df['PositionX'], df['PositionY'],
             label='Trajectory', color='blue', linestyle='--', linewidth=2, alpha=0.7)

    plt.plot(df_estimation['estimatedX'], df_estimation['estimatedY'],
                 label='Estimated Trajectory', color='red', linestyle='--', linewidth=2, alpha=0.7)

    # Initial point
    plt.scatter(df['PositionX'].iloc[0], df['PositionY'].iloc[0],
                color='green', s=100, label='Start', zorder=5, edgecolors='black')

    # Final point
    plt.scatter(df['PositionX'].iloc[-1], df['PositionY'].iloc[-1],
                color='red', s=100, label='End', zorder=5, edgecolors='black')

    plt.xlim(0, side_length)
    plt.ylim(0, side_length)

    plt.title(f'Trajectory', fontsize=14)
    plt.xlabel('X (m)')
    plt.ylabel('Y (m)')

    plt.grid(True, linestyle='--', alpha=0.6)

    plt.legend()

    plt.gca().set_aspect('equal', adjustable='box')

    plt.tight_layout()
    plt.savefig(f'charts/trajectory{name}.pdf')

if __name__ == '__main__':

    centralized = False
    Path('charts').mkdir(parents=True, exist_ok=True)

    if centralized:
        generate_charts()
    else:
        num_nodes = 4
        for i in range(num_nodes):
            generate_charts(name=f'_node-{i}')
