#!/usr/bin/env node
/**
 * convert_all.js
 * 
 * Convierte los 6 bbmodel seleccionados a OBJ y los pone en el directorio
 * correcto de assets del mod.
 */

const { execSync } = require('child_process');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const CONVERTER = path.join(__dirname, 'bbmodel_to_obj.js');
const MODELS_OUT = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'notevayas', 'models', 'block');

// Mapeo: nombre del bbmodel en la raiz -> nombre del modelo de salida
const MODELS = [
    { src: 'planta_v7_2ramas.bbmodel',           out: 'cultivo_stage0' },
    { src: 'planta_v6_3ramas.bbmodel',            out: 'cultivo_stage2' },
    { src: 'planta_v5_60pct_chica.bbmodel',       out: 'cultivo_stage3' },
    { src: 'planta_v4_tallo_fino_chica.bbmodel',  out: 'cultivo_stage4' },
    { src: 'planta_v2_tallo_fino.bbmodel',        out: 'cultivo_stage5' },
    { src: 'planta_v3_ramas_cogollos.bbmodel',    out: 'cultivo_stage7' },
];

for (const m of MODELS) {
    const inputPath = path.join(ROOT, m.src);
    const cmd = `node "${CONVERTER}" "${inputPath}" "${MODELS_OUT}" "${m.out}"`;
    console.log(`\n=== ${m.src} -> ${m.out} ===`);
    try {
        const output = execSync(cmd, { encoding: 'utf8' });
        console.log(output);
    } catch (e) {
        console.error('ERROR:', e.message);
        process.exit(1);
    }
}

console.log('\n=== Listo! ===');
console.log('Modelos OBJ generados en:', MODELS_OUT);
