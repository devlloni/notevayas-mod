#!/usr/bin/env node
/**
 * bbmodel_to_mc_json.js
 *
 * Convierte archivos .bbmodel de Blockbench a modelos JSON de Minecraft.
 * 
 * Estrategia para rotaciones:
 * - Minecraft solo permite UNA rotación por elemento, en UN eje, en múltiplos de 22.5°
 * - Los bbmodels tienen rotaciones de grupo anidadas con ángulos arbitrarios
 * - Para cada cubo, se acumulan todas las rotaciones de grupo + elemento
 * - Si la rotación resultante es solo en un eje, se usa directamente (snapped a 22.5°)
 * - Si es multi-eje, se bakea la rotación en los vértices y se usa el AABB resultante
 *   con la rotación dominante preservada
 *
 * Uso:
 *   node bbmodel_to_mc_json.js <input.bbmodel> <output.json> [texture_id]
 */

const fs = require('fs');
const path = require('path');

// ─────────────────────────────────── math

function degToRad(d) { return d * Math.PI / 180; }
function radToDeg(r) { return r * 180 / Math.PI; }

function rotX(a) { const c=Math.cos(a),s=Math.sin(a); return [[1,0,0],[0,c,-s],[0,s,c]]; }
function rotY(a) { const c=Math.cos(a),s=Math.sin(a); return [[c,0,s],[0,1,0],[-s,0,c]]; }
function rotZ(a) { const c=Math.cos(a),s=Math.sin(a); return [[c,-s,0],[s,c,0],[0,0,1]]; }

function matMul(a, b) {
    const r = [[0,0,0],[0,0,0],[0,0,0]];
    for (let i=0;i<3;i++) for (let j=0;j<3;j++) for (let k=0;k<3;k++) r[i][j]+=a[i][k]*b[k][j];
    return r;
}

function matVec(m, v) {
    return [m[0][0]*v[0]+m[0][1]*v[1]+m[0][2]*v[2],
            m[1][0]*v[0]+m[1][1]*v[1]+m[1][2]*v[2],
            m[2][0]*v[0]+m[2][1]*v[1]+m[2][2]*v[2]];
}

function buildRotMat(euler) {
    if (!euler || (euler[0]===0 && euler[1]===0 && euler[2]===0)) return [[1,0,0],[0,1,0],[0,0,1]];
    let m = rotY(degToRad(euler[1]));
    m = matMul(m, rotX(degToRad(euler[0])));
    m = matMul(m, rotZ(degToRad(euler[2])));
    return m;
}

function rotatePoint(p, origin, mat) {
    const l = [p[0]-origin[0], p[1]-origin[1], p[2]-origin[2]];
    const r = matVec(mat, l);
    return [r[0]+origin[0], r[1]+origin[1], r[2]+origin[2]];
}

/** Snap a degree value to the nearest 22.5° multiple, clamped to [-45, 45] */
function snap225(deg) {
    const snapped = Math.round(deg / 22.5) * 22.5;
    return Math.max(-45, Math.min(45, snapped));
}

// ─────────────────────────────────── bbmodel processing

function cubeVerts(from, to) {
    const [x0,y0,z0] = from;
    const [x1,y1,z1] = to;
    return [
        [x0,y0,z0],[x1,y0,z0],[x1,y0,z1],[x0,y0,z1],
        [x0,y1,z0],[x1,y1,z0],[x1,y1,z1],[x0,y1,z1],
    ];
}

/**
 * Compute the compound rotation of an element by walking its group hierarchy.
 * Returns { totalEulerApprox, origin, needsBake }
 */
function computeRotation(el, rotStack) {
    // Collect all rotations: element's own + all group rotations
    const rotations = [];
    
    // Element's own rotation
    if (el.rotation && (el.rotation[0]!==0 || el.rotation[1]!==0 || el.rotation[2]!==0)) {
        rotations.push({ euler: el.rotation, origin: el.origin || [8,8,8] });
    }
    
    // Group rotations (innermost to outermost)
    for (let i = rotStack.length - 1; i >= 0; i--) {
        rotations.push(rotStack[i]);
    }
    
    if (rotations.length === 0) {
        return { rotation: null, needsBake: false };
    }
    
    // If there's exactly one rotation and it's single-axis, we can use MC's rotation
    if (rotations.length === 1) {
        const r = rotations[0];
        const axes = [r.euler[0]!==0, r.euler[1]!==0, r.euler[2]!==0];
        const axisCount = axes.filter(x=>x).length;
        if (axisCount <= 1) {
            const axis = axes[0] ? 'x' : (axes[1] ? 'y' : 'z');
            const angle = r.euler[axes[0] ? 0 : (axes[1] ? 1 : 2)];
            return {
                rotation: { angle: snap225(angle), axis, origin: r.origin },
                needsBake: Math.abs(angle - snap225(angle)) > 0.5
            };
        }
    }
    
    // Multiple rotations or multi-axis: need to bake
    return { rotation: null, needsBake: true, rotations };
}

/**
 * Process a single element, producing a Minecraft JSON element.
 */
function processElement(el, rotStack, texWidth, texHeight) {
    const rotInfo = computeRotation(el, rotStack);
    
    let from = [...el.from];
    let to = [...el.to];
    let mcRotation = rotInfo.rotation;
    
    if (rotInfo.needsBake) {
        // Bake all rotations into the vertices
        let verts = cubeVerts(el.from, el.to);
        
        // Apply element's own rotation
        if (el.rotation && (el.rotation[0]!==0 || el.rotation[1]!==0 || el.rotation[2]!==0)) {
            const mat = buildRotMat(el.rotation);
            const origin = el.origin || [8,8,8];
            verts = verts.map(v => rotatePoint(v, origin, mat));
        }
        
        // Apply group rotations (innermost first)
        for (let i = rotStack.length - 1; i >= 0; i--) {
            const { euler, origin } = rotStack[i];
            const mat = buildRotMat(euler);
            verts = verts.map(v => rotatePoint(v, origin, mat));
        }
        
        // Compute AABB of the rotated cube
        const mins = [Infinity, Infinity, Infinity];
        const maxs = [-Infinity, -Infinity, -Infinity];
        for (const v of verts) {
            for (let i = 0; i < 3; i++) {
                mins[i] = Math.min(mins[i], v[i]);
                maxs[i] = Math.max(maxs[i], v[i]);
            }
        }
        
        from = mins.map(v => Math.round(v * 1000) / 1000);
        to = maxs.map(v => Math.round(v * 1000) / 1000);
        mcRotation = null; // rotation already baked
    }
    
    // Clamp to Minecraft's -16..32 range
    from = from.map(v => Math.max(-16, Math.min(32, v)));
    to = to.map(v => Math.max(-16, Math.min(32, v)));
    
    // Build faces
    const faces = {};
    const faceNames = ['north', 'east', 'south', 'west', 'up', 'down'];
    for (const fn of faceNames) {
        const bbFace = el.faces[fn];
        if (!bbFace || bbFace.texture === null || bbFace.texture === undefined || bbFace.texture === -1) continue;
        
        const face = { texture: '#0' };
        if (bbFace.uv) {
            // Minecraft uses pixel coords directly (same as bbmodel for 16x16 textures)
            // But our texture is 128x64, so we keep pixel coords as-is
            face.uv = [...bbFace.uv];
        }
        if (bbFace.cullface) {
            face.cullface = bbFace.cullface;
        }
        faces[fn] = face;
    }
    
    if (Object.keys(faces).length === 0) return null;
    
    const element = { from, to, faces };
    
    if (mcRotation) {
        element.rotation = {
            origin: mcRotation.origin.map(v => Math.round(v * 1000) / 1000),
            axis: mcRotation.axis,
            angle: mcRotation.angle,
        };
        if (mcRotation.angle !== 0 && Math.abs(mcRotation.angle) % 22.5 === 0) {
            element.rotation.rescale = false;
        }
    }
    
    return element;
}

// ─────────────────────────────────── outliner walker

function walkOutliner(nodes, elementMap, parentRotations, results, texW, texH) {
    for (const node of nodes) {
        if (typeof node === 'string') {
            const el = elementMap.get(node);
            if (!el || el.type !== 'cube') continue;
            if (el.visibility === false) continue;
            const mcEl = processElement(el, parentRotations, texW, texH);
            if (mcEl) results.push(mcEl);
        } else if (typeof node === 'object' && node.children) {
            if (node.visibility === false) continue;
            const groupRots = [...parentRotations];
            if (node.rotation && (node.rotation[0]!==0 || node.rotation[1]!==0 || node.rotation[2]!==0)) {
                groupRots.push({ euler: node.rotation, origin: node.origin || [0,0,0] });
            }
            walkOutliner(node.children, elementMap, groupRots, results, texW, texH);
        }
    }
}

// ─────────────────────────────────── texture extraction

function extractTexture(bbmodel, outputPath) {
    if (fs.existsSync(outputPath)) {
        console.log('  Textura ya existe: ' + outputPath);
        return;
    }
    if (!bbmodel.textures || bbmodel.textures.length === 0) return;
    const tex = bbmodel.textures[0];
    if (!tex.source) return;
    const base64 = tex.source.replace(/^data:image\/\w+;base64,/, '');
    const buffer = Buffer.from(base64, 'base64');
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });
    fs.writeFileSync(outputPath, buffer);
    console.log('  Textura extraida: ' + outputPath + ' (' + buffer.length + ' bytes)');
}

// ─────────────────────────────────── main

function convert(inputPath, outputPath, textureRef) {
    console.log('Convirtiendo: ' + inputPath);
    const bbmodel = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
    
    const elementMap = new Map();
    for (const el of bbmodel.elements) elementMap.set(el.uuid, el);
    
    const texW = bbmodel.resolution.width;
    const texH = bbmodel.resolution.height;
    
    const mcElements = [];
    walkOutliner(bbmodel.outliner, elementMap, [], mcElements, texW, texH);
    
    console.log('  Elementos MC: ' + mcElements.length);
    
    // Build MC JSON model
    const model = {
        credit: 'Generado desde bbmodel por bbmodel_to_mc_json.js',
        texture_size: [texW, texH],
        textures: {
            '0': textureRef || 'notevayas:block/planta_cannabis',
            particle: textureRef || 'notevayas:block/planta_cannabis'
        },
        elements: mcElements,
        // No parent - standalone model. No ambient occlusion for plants.
        ambientocclusion: false,
    };
    
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });
    fs.writeFileSync(outputPath, JSON.stringify(model, null, 2), 'utf8');
    console.log('  OK -> ' + outputPath);
    
    return bbmodel;
}

// ─────────────────────────────────── batch mode

if (process.argv.length >= 4) {
    const inputPath = process.argv[2];
    const outputPath = process.argv[3];
    const textureRef = process.argv[4] || 'notevayas:block/planta_cannabis';
    convert(inputPath, outputPath, textureRef);
} else if (process.argv[2] === '--batch') {
    // Batch mode: convert all models (11 stages)
    const ROOT = path.resolve(__dirname, '..');
    const MODELS_SRC = path.join(ROOT, 'models-src');
    const MODELS_OUT = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'notevayas', 'models', 'block');
    const TEX_OUT = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'notevayas', 'textures', 'block', 'planta_cannabis.png');
    
    const MODELS = [
        { src: 'planta_v7_2ramas.bbmodel',                    out: 'cultivo_stage0.json' },
        { src: 'planta_v6_3ramas.bbmodel',                    out: 'cultivo_stage2.json' },
        { src: 'planta_v5_60pct_chica.bbmodel',               out: 'cultivo_stage3.json' },
        { src: 'planta_v4_tallo_fino_chica.bbmodel',          out: 'cultivo_stage4.json' },
        { src: 'planta_v2_tallo_fino.bbmodel',                out: 'cultivo_stage5.json' },
        { src: 'planta_v3_ramas_cogollos.bbmodel',            out: 'cultivo_stage7.json' },
        { src: 'planta_v3_cogollos_1_falta_madurar.bbmodel',  out: 'cultivo_stage8.json' },
        { src: 'planta_v3_cogollos_2_perfecto.bbmodel',       out: 'cultivo_stage9.json' },
        { src: 'planta_v3_cogollos_3_super_maduro.bbmodel',   out: 'cultivo_stage10.json' },
    ];
    
    let firstBBModel = null;
    for (const m of MODELS) {
        console.log('\n=== ' + m.src + ' -> ' + m.out + ' ===');
        const bbmodel = convert(
            path.join(MODELS_SRC, m.src),
            path.join(MODELS_OUT, m.out),
            'notevayas:block/planta_cannabis'
        );
        if (!firstBBModel) firstBBModel = bbmodel;
    }
    
    // Extract texture once
    if (firstBBModel) {
        extractTexture(firstBBModel, TEX_OUT);
    }
    
    console.log('\n=== Listo! ===');
} else {
    console.log('Uso:');
    console.log('  node bbmodel_to_mc_json.js <input.bbmodel> <output.json> [texture_ref]');
    console.log('  node bbmodel_to_mc_json.js --batch');
    process.exit(1);
}
