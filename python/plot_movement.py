import re
import numpy as np
import pandas as pd
from pathlib import Path
import matplotlib.pyplot as plt
import matplotlib

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

def generate_charts(df_true, df_estimation, name, charts_path):

    side_length = 100

    plt.figure(figsize=(10, 10))

    plt.plot(df_true['PositionX'], df_true['PositionY'],
             label='Real Trajectory', color='blue', linestyle='--', linewidth=4, alpha=0.7)

    plt.plot(df_estimation['estimatedX'], df_estimation['estimatedY'],
                 label='Estimated Trajectory', color='red', linestyle='--', linewidth=4, alpha=0.7)

    # Initial point
    plt.scatter(df_true['PositionX'].iloc[0], df_true['PositionY'].iloc[0],
                color='green', s=200, label='Start', zorder=5, edgecolors='black')

    # Final point
    plt.scatter(df_true['PositionX'].iloc[-1], df_true['PositionY'].iloc[-1],
                color='red', s=200, label='End', zorder=5, edgecolors='black')

    plt.xlim(0, side_length)
    plt.ylim(0, side_length)

    #plt.title(f'Trajectory')
    plt.xlabel('X (m)', fontsize=40)
    plt.ylabel('Y (m)', fontsize=40)

    plt.grid(True, linestyle='--', alpha=0.6)

    plt.legend(fontsize=30, title_fontsize=22)

    plt.gca().set_aspect('equal', adjustable='box')

    plt.xticks(fontsize=30)
    plt.yticks(fontsize=30)

    plt.tight_layout()
    plt.savefig(f'{charts_path}/trajectory{name}.pdf')

if __name__ == '__main__':

    experiments = [('grid1x1', 1), ('grid2x1', 2), ('grid3x3', 9),('grid5x5', 25)]

    seed_to_plot = 42

    for experiment, num_sensors in experiments:
        charts_path = f'charts/seed-{seed_to_plot}/{experiment}'
        Path(charts_path).mkdir(parents=True, exist_ok=True)
        data_path = f'data-{experiment}'

        df_true = read_alchemist_csv(f'{data_path}/track-movement-neighboring-aggregation_seed-{seed_to_plot}.0.csv')

        dfs = []

        for i in range(num_sensors):
            df_estimation = pd.read_csv(f'{data_path}/estimations_node-{i}_seed-{seed_to_plot}.0.csv')
            generate_charts(df_true, df_estimation, f'node-{i}', charts_path)
            dfs.append(df_estimation)

        df_estimation_aggregated = pd.concat(dfs).groupby(level=0).mean()
        generate_charts(df_true, df_estimation_aggregated, f'-{num_sensors}sensors', charts_path)

