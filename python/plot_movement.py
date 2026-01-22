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

def read_alchemist_csv(path):
    lines = np.matrix(openCsv(path))
    vars =  extractVariableNames(path)
    vars = [v.split('[')[0] for v in vars]
    df = pd.DataFrame(data=lines, columns=vars)
    df = df.dropna()
    return df

def generate_charts(df_true, df_estimation, name):

    side_length = 90

    plt.figure(figsize=(10, 10))

    plt.plot(df_true['PositionX'], df_true['PositionY'],
             label='Trajectory', color='blue', linestyle='--', linewidth=2, alpha=0.7)

    plt.plot(df_estimation['estimatedX'], df_estimation['estimatedY'],
                 label='Estimated Trajectory', color='red', linestyle='--', linewidth=2, alpha=0.7)

    # Initial point
    plt.scatter(df_true['PositionX'].iloc[0], df_true['PositionY'].iloc[0],
                color='green', s=100, label='Start', zorder=5, edgecolors='black')

    # Final point
    plt.scatter(df_true['PositionX'].iloc[-1], df_true['PositionY'].iloc[-1],
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

    Path('charts').mkdir(parents=True, exist_ok=True)
    data_path = 'data'
    num_sensors = 9

    df_true = read_alchemist_csv(f'{data_path}/track-movement-neighboring-aggregation/track-movement-neighboring-aggregation_seed-42.0.csv')

    dfs = []

    for i in range(num_sensors):
        df_estimation = pd.read_csv(f'{data_path}/estimations_node-{i}.csv')
        generate_charts(df_true, df_estimation, f'node-{i}')
        dfs.append(df_estimation)

    df_estimation_aggregated = pd.concat(dfs).groupby(level=0).mean()
    generate_charts(df_true, df_estimation_aggregated, f'aggregated')

