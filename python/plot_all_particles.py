import numpy as np
import pandas as pd
from pathlib import Path
import matplotlib.pyplot as plt

def plot_particle_estimates(df, step=100, output_dir=None):

    num_particles = 250
    indices_to_plot = range(0, len(df), step)

    for row_idx in indices_to_plot:
        row = df.iloc[row_idx]

        plt.figure(figsize=(10, 8))

        xs = []
        ys = []
        ws = []

        for i in range(num_particles):
            x = row[f'p_{i}-X']
            y = row[f'p_{i}-Y']
            w = row[f'p_{i}-W']

            xs.append(x)
            ys.append(y)
            ws.append(w)

        sizes = [max(0.1, weight * 1000) for weight in ws]

        plt.scatter(xs, ys, s=sizes, alpha=0.6, edgecolors='none', color='blue')

        plt.title(f"Particle Filter Distribution - Timestep {row_idx}")
        plt.xlabel("X (m)")
        plt.ylabel("Y (m)")
        plt.grid(True, linestyle='--', alpha=0.5)

        filename = f"{output_dir}step_{row_idx}.pdf"
        plt.savefig(filename)
        plt.close() # Chiude la figura per liberare memoria

if __name__ == '__main__':
    charts_path = 'charts/allparticles/'
    Path(charts_path).mkdir(parents=True, exist_ok=True)

    nodes = 4

    for i in range(nodes):
        path = f'{charts_path}/node-{i}/'
        Path(path).mkdir(parents=True, exist_ok=True)

        df = pd.read_csv(f'data/particles_node-{i}.csv')

        plot_particle_estimates(df, 100, path)