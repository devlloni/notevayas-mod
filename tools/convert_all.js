#!/usr/bin/env node
/**
 * convert_all.js
 * 
 * Convierte los bbmodel del ciclo de vida completo de la planta a JSON de
 * Minecraft y los pone en el directorio correcto de assets del mod.
 *
 * Ciclo de vida (11 etapas, 0-10):
 *   0-1: brote (2 ramas)
 *   2:   plántula (3 ramas)
 *   3:   vegetativo temprano (60% tamaño)
 *   4:   vegetativo (tallo fino chica)
 *   5-6: vegetativo maduro (tallo fino completo)
 *   7:   floración (ramas con cogollos)
 *   8:   maduración temprana (cogollos sin madurar)
 *   9:   maduración perfecta (cogollos perfectos)
 *  10:   super maduro (cogollos super maduros)
 */

const { execSync } = require('child_process');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const CONVERTER = path.join(__dirname, 'bbmodel_to_mc_json.js');
const MODELS_SRC = path.join(ROOT, 'models-src');
const MODELS_OUT = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'notevayas', 'models', 'block');

// Mapeo: nombre del bbmodel en models-src/ -> nombre del modelo de salida
const MODELS = [
    { src: 'planta_v7_2ramas.bbmodel',                    out: 'cultivo_stage0' },
    { src: 'planta_v6_3ramas.bbmodel',                    out: 'cultivo_stage2' },
    { src: 'planta_v5_60pct_chica.bbmodel',               out: 'cultivo_stage3' },
    { src: 'planta_v4_tallo_fino_chica.bbmodel',          out: 'cultivo_stage4' },
    { src: 'planta_v2_tallo_fino.bbmodel',                out: 'cultivo_stage5' },
    { src: 'planta_v3_ramas_cogollos.bbmodel',            out: 'cultivo_stage7' },
    { src: 'planta_v3_cogollos_1_falta_madurar.bbmodel',  out: 'cultivo_stage8' },
    { src: 'planta_v3_cogollos_2_perfecto.bbmodel',       out: 'cultivo_stage9' },
    { src: 'planta_v3_cogollos_3_super_maduro.bbmodel',   out: 'cultivo_stage10' },
];

for (const m of MODELS) {
    const inputPath = path.join(MODELS_SRC, m.src);
    const outputPath = path.join(MODELS_OUT, m.out + '.json');
    const cmd = `node "${CONVERTER}" "${inputPath}" "${outputPath}" "notevayas:block/planta_cannabis"`;
    console.log(`\n=== ${m.src} -> ${m.out} ===`);
    try {
        const output = execSync(cmd, { encoding: 'utf8' });
        console.log(output);
    } catch (e) {
        console.error('ERROR:', e.message);
        process.exit(1);
    }
}

// stage1 = copia de stage0 (misma planta, solo cambia el blockstate)
const fs = require('fs');
const stage0 = path.join(MODELS_OUT, 'cultivo_stage0.json');
const stage1 = path.join(MODELS_OUT, 'cultivo_stage1.json');
fs.copyFileSync(stage0, stage1);
console.log('\n=== cultivo_stage0.json -> cultivo_stage1.json (copia) ===');

// stage6 = copia de stage5 (misma planta vegetativa madura)
const stage5 = path.join(MODELS_OUT, 'cultivo_stage5.json');
const stage6 = path.join(MODELS_OUT, 'cultivo_stage6.json');
fs.copyFileSync(stage5, stage6);
console.log('=== cultivo_stage5.json -> cultivo_stage6.json (copia) ===');

console.log('\n=== Listo! ===');
console.log('Modelos JSON generados en:', MODELS_OUT);
