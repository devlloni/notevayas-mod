#!/usr/bin/env node
/**
 * bbmodel_to_obj.js
 *
 * Convierte archivos .bbmodel de Blockbench (formato "free") a Wavefront OBJ,
 * aplicando ("bakeando") las rotaciones de grupos del outliner directamente
 * en las coordenadas de los vértices. También extrae la textura embebida como PNG.
 *
 * Uso:
 *   node bbmodel_to_obj.js <input.bbmodel> <output_dir> <model_name>
 *
 * Genera:
 *   <output_dir>/<model_name>.obj
 *   <output_dir>/<model_name>.mtl
 *   <output_dir>/planta_cannabis.png  (textura extraída, una sola vez)
 */

const fs = require('fs');
const path = require('path');

// ─────────────────────────────────── math helpers

function degToRad(d) { return d * Math.PI / 180; }

function rotX(angle) {
    const c = Math.cos(angle), s = Math.sin(angle);
    return [[1,0,0],[0,c,-s],[0,s,c]];
}

function rotY(angle) {
    const c = Math.cos(angle), s = Math.sin(angle);
    return [[c,0,s],[0,1,0],[-s,0,c]];
}

function rotZ(angle) {
    const c = Math.cos(angle), s = Math.sin(angle);
    return [[c,-s,0],[s,c,0],[0,0,1]];
}

function matMul(a, b) {
    const r = [[0,0,0],[0,0,0],[0,0,0]];
    for (let i = 0; i < 3; i++)
        for (let j = 0; j < 3; j++)
            for (let k = 0; k < 3; k++)
                r[i][j] += a[i][k] * b[k][j];
    return r;
}

function matVec(m, v) {
    return [
        m[0][0]*v[0] + m[0][1]*v[1] + m[0][2]*v[2],
        m[1][0]*v[0] + m[1][1]*v[1] + m[1][2]*v[2],
        m[2][0]*v[0] + m[2][1]*v[1] + m[2][2]*v[2],
    ];
}

const IDENTITY = [[1,0,0],[0,1,0],[0,0,1]];

/**
 * Build rotation matrix from Blockbench euler angles [rx, ry, rz] in degrees.
 * Blockbench applies: Y * X * Z
 */
function buildRotationMatrix(eulerDeg) {
    if (!eulerDeg || (eulerDeg[0] === 0 && eulerDeg[1] === 0 && eulerDeg[2] === 0)) {
        return IDENTITY;
    }
    let m = rotY(degToRad(eulerDeg[1]));
    m = matMul(m, rotX(degToRad(eulerDeg[0])));
    m = matMul(m, rotZ(degToRad(eulerDeg[2])));
    return m;
}

function rotateAroundOrigin(point, origin, matrix) {
    const local = [point[0] - origin[0], point[1] - origin[1], point[2] - origin[2]];
    const rotated = matVec(matrix, local);
    return [rotated[0] + origin[0], rotated[1] + origin[1], rotated[2] + origin[2]];
}

// ─────────────────────────────────── bbmodel parser

function parseBBModel(filePath) {
    return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function buildElementMap(elements) {
    const map = new Map();
    for (const el of elements) map.set(el.uuid, el);
    return map;
}

/**
 * 8 vertices of a cube from `from` and `to` AABBs.
 */
function cubeVertices(from, to) {
    const [x0, y0, z0] = from;
    const [x1, y1, z1] = to;
    return [
        [x0, y0, z0], [x1, y0, z0], [x1, y0, z1], [x0, y0, z1],
        [x0, y1, z0], [x1, y1, z0], [x1, y1, z1], [x0, y1, z1],
    ];
}

// Face definitions for OBJ (CCW winding when viewed from outside)
const CUBE_FACES_OBJ = [
    { name: 'north', verts: [5, 4, 0, 1] },
    { name: 'south', verts: [3, 7, 6, 2] },
    { name: 'east',  verts: [2, 6, 5, 1] },
    { name: 'west',  verts: [0, 4, 7, 3] },
    { name: 'up',    verts: [5, 4, 7, 6] },
    { name: 'down',  verts: [0, 1, 2, 3] },
];

/**
 * Convert bbmodel pixel UVs [u1,v1,u2,v2] to OBJ normalized UVs.
 */
function faceUVs(face, texWidth, texHeight) {
    if (!face || !face.uv) return null;
    const [u1, v1, u2, v2] = face.uv;
    const nu1 = u1 / texWidth;
    const nv1 = 1.0 - v1 / texHeight;
    const nu2 = u2 / texWidth;
    const nv2 = 1.0 - v2 / texHeight;
    return [
        [nu2, nv2], [nu1, nv2], [nu1, nv1], [nu2, nv1],
    ];
}

// ─────────────────────────────────── outliner traversal

function walkOutliner(nodes, elementMap, parentRotations, result, texWidth, texHeight) {
    for (const node of nodes) {
        if (typeof node === 'string') {
            const el = elementMap.get(node);
            if (!el || el.type !== 'cube') continue;
            if (el.visibility === false) continue;
            processElement(el, parentRotations, result, texWidth, texHeight);
        } else if (typeof node === 'object' && node.children) {
            if (node.visibility === false) continue;
            const groupRotations = [...parentRotations];
            if (node.rotation && (node.rotation[0] !== 0 || node.rotation[1] !== 0 || node.rotation[2] !== 0)) {
                const mat = buildRotationMatrix(node.rotation);
                const origin = node.origin || [0, 0, 0];
                groupRotations.push({ origin, matrix: mat });
            }
            walkOutliner(node.children, elementMap, groupRotations, result, texWidth, texHeight);
        }
    }
}

function processElement(el, rotationStack, result, texWidth, texHeight) {
    let verts = cubeVertices(el.from, el.to);

    // Apply element's own rotation
    if (el.rotation && (el.rotation[0] !== 0 || el.rotation[1] !== 0 || el.rotation[2] !== 0)) {
        const mat = buildRotationMatrix(el.rotation);
        const origin = el.origin || [8, 8, 8];
        verts = verts.map(v => rotateAroundOrigin(v, origin, mat));
    }

    // Apply group rotations (innermost first)
    for (let i = rotationStack.length - 1; i >= 0; i--) {
        const { origin, matrix } = rotationStack[i];
        verts = verts.map(v => rotateAroundOrigin(v, origin, matrix));
    }

    // Convert from Blockbench pixel coords (0-16) to OBJ block coords (-0.5 to 0.5)
    const baseVertexIdx = result.vertices.length;
    for (const v of verts) {
        result.vertices.push([
            v[0] / 16.0 - 0.5,
            v[1] / 16.0,
            v[2] / 16.0 - 0.5,
        ]);
    }

    // Generate faces with UVs
    for (const face of CUBE_FACES_OBJ) {
        const bbFace = el.faces[face.name];
        if (!bbFace || bbFace.texture === null || bbFace.texture === undefined || bbFace.texture === -1) continue;

        const uvs = faceUVs(bbFace, texWidth, texHeight);
        if (!uvs) continue;

        const uvIndices = [];
        for (const uv of uvs) {
            result.uvs.push(uv);
            uvIndices.push(result.uvs.length);
        }

        result.faces.push({
            v: face.verts.map(i => baseVertexIdx + i + 1),
            vt: uvIndices,
        });
    }
}

// ─────────────────────────────────── OBJ/MTL writer

function writeOBJ(result, objPath, mtlName, modelName) {
    const lines = [];
    lines.push('# Generated from bbmodel by bbmodel_to_obj.js');
    lines.push('# Model: ' + modelName);
    lines.push('mtllib ' + mtlName);
    lines.push('o ' + modelName);
    lines.push('');
    for (const v of result.vertices)
        lines.push('v ' + v[0].toFixed(6) + ' ' + v[1].toFixed(6) + ' ' + v[2].toFixed(6));
    lines.push('');
    for (const uv of result.uvs)
        lines.push('vt ' + uv[0].toFixed(6) + ' ' + uv[1].toFixed(6));
    lines.push('');
    lines.push('usemtl planta_cannabis');
    lines.push('');
    for (const face of result.faces) {
        const parts = [];
        for (let i = 0; i < face.v.length; i++)
            parts.push(face.v[i] + '/' + face.vt[i]);
        lines.push('f ' + parts.join(' '));
    }
    fs.writeFileSync(objPath, lines.join('\n'), 'utf8');
}

function writeMTL(mtlPath, textureName) {
    const lines = [
        '# Material for cannabis plant model',
        'newmtl planta_cannabis',
        'Ka 1.000 1.000 1.000',
        'Kd 1.000 1.000 1.000',
        'Ks 0.000 0.000 0.000',
        'd 1.0',
        'illum 1',
        'map_Kd ' + textureName,
    ];
    fs.writeFileSync(mtlPath, lines.join('\n'), 'utf8');
}

// ─────────────────────────────────── texture extraction

function extractTexture(bbmodel, outputDir) {
    const texFile = path.join(outputDir, 'planta_cannabis.png');
    if (fs.existsSync(texFile)) {
        console.log('  Textura ya existe, saltando.');
        return;
    }
    if (!bbmodel.textures || bbmodel.textures.length === 0) {
        console.error('  No hay texturas embebidas.');
        return;
    }
    const tex = bbmodel.textures[0];
    if (!tex.source) {
        console.error('  La textura no tiene data embebida.');
        return;
    }
    const base64 = tex.source.replace(/^data:image\/\w+;base64,/, '');
    const buffer = Buffer.from(base64, 'base64');
    fs.writeFileSync(texFile, buffer);
    console.log('  Textura extraida: ' + texFile + ' (' + buffer.length + ' bytes)');
}

// ─────────────────────────────────── main

function convert(inputPath, outputDir, modelName) {
    console.log('Convirtiendo: ' + inputPath + ' -> ' + modelName);
    const bbmodel = parseBBModel(inputPath);
    const elementMap = buildElementMap(bbmodel.elements);
    const texWidth = bbmodel.resolution.width;
    const texHeight = bbmodel.resolution.height;

    const result = { vertices: [], uvs: [], faces: [] };
    walkOutliner(bbmodel.outliner, elementMap, [], result, texWidth, texHeight);

    console.log('  Vertices: ' + result.vertices.length +
                ', UVs: ' + result.uvs.length +
                ', Caras: ' + result.faces.length);

    fs.mkdirSync(outputDir, { recursive: true });
    const mtlName = modelName + '.mtl';
    const objPath = path.join(outputDir, modelName + '.obj');
    const mtlPath = path.join(outputDir, mtlName);

    writeOBJ(result, objPath, mtlName, modelName);
    writeMTL(mtlPath, 'planta_cannabis.png');
    extractTexture(bbmodel, outputDir);
    console.log('  OK -> ' + objPath);
}

if (process.argv.length < 5) {
    console.log('Uso: node bbmodel_to_obj.js <input.bbmodel> <output_dir> <model_name>');
    process.exit(1);
}

convert(process.argv[2], process.argv[3], process.argv[4]);
