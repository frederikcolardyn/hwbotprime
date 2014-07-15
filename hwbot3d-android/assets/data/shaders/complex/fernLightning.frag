#ifdef GL_ES
precision mediump float;
#endif

/*
 * fernLightning (BabyRabbit)
 * - attempt to do 2d global illumination (assumes reflective objects)
 * - caustics!
 */

uniform float time;
uniform vec2 mouse;
uniform vec2 resolution;

#define PI2 6.28318530718


// samples radiating from point
#define SAMPLES360 30
// max steps along ray
#define MAXSTEPS 60

#define MAXBOUNCES 2

#define MINDISTANCE 0.001

// angle each sample covers
#define DA (PI2/float(SAMPLES360))

float rand(vec2 n) {
    return fract(sin(dot(n.xy, vec2(12.9898, 78.233)))* 43758.5453);
}

float box(vec2 p, vec2 o, vec2 b) {
    return length(max(abs(p-o)-b,0.0));
}

float ucircle(vec2 p, vec2 o, float r) {
    return abs(length(p-o)-r);
}

float scircle(vec2 p, vec2 o, float r) {
    return length(p-o)-r;
}




vec2 l_pos = vec2(mouse.x, mouse.y*resolution.y/resolution.x);

vec2 c_pos = vec2(0.5,0.5*resolution.y/resolution.x);// + vec2(sin(time), cos(time))*0.1;

vec2 s_pos = vec2(0.5,0.5*resolution.y/resolution.x);



// use distance functions!
float scene(vec2 p) {
    float d1 = scircle(p, c_pos, 0.04);
    float d2 = box(p, vec2(0.2, 0.2), vec2(0.2, 0.04));
    float d3 = ucircle(p, s_pos, 0.5);
    
    return min(d1,min(d2, d3));
}

vec3 ambientColor(vec2 p) {
    float d = scene(p);
    if(d <= MINDISTANCE) return vec3(0.3, 0.4, 0.5); // objects
    return vec3(0.1); // floor
}



vec2 sceneNormal(vec2 p) {
    float e = 0.00001;
    float dx = scene(p-vec2(e,0.0)) - scene(p+vec2(e,0.0));
    float dy = scene(p-vec2(0.0, e)) - scene(p+vec2(0.0,e));
    return normalize(vec2(dx,dy));
}


// get light from light source if within sample cone
float getLight(vec2 p ,vec2 v, float t, float ts) {
    float d = length(l_pos-p);
    float a = acos(dot(v, (l_pos-p)/d));
    if(d < t && a < DA*0.5) {
        return float(SAMPLES360)/(d+ts);
    }
    return 0.0;
}

// intersect with scene, everything is perfectly reflective
float traceRay(vec2 p, vec2 v) {
    float ts = 0.0;
    float light = 0.0;
    int bounces = 0;
    float t = 0.0;
    for (int i = 0 ; i < MAXSTEPS ; i++) {
        float dt = scene(p + v*t);
        if(dt < MINDISTANCE) { // hit object
            
            // check if passed light source
            light += getLight(p, v, t, ts);
            
            // reflect the ray
            p += v*t;
            v = reflect(v, sceneNormal(p));
            ts = t;
            t = MINDISTANCE; // ensure don’t collide with surface
            
            bounces++;
            if(bounces >= MAXBOUNCES) break;
        }
        t += dt;
    }
    if(bounces == 0) {
        // might still have passed light source
        light += getLight(p, v, t, ts);
    }
    return light; // hit nothing
}

float castRays(vec2 p){
    // cast rays in all directions out from point
    float c = 0.0;
    float a = rand(p)*DA;
    for (int i = 0 ; i < SAMPLES360 ; i++) {
        a += DA;
        vec2 v = vec2(sin(a), cos(a));
        c += traceRay(p, v);
    }
    c /= float(SAMPLES360);
    return c;
}

void main( void ) {
    vec2 p = gl_FragCoord.xy/resolution.xy;
    p.y *= resolution.y/resolution.x;
    
    vec3 color = ambientColor(p) + castRays(p) * 0.04 * vec3(1.0,1.0,0.6);
    color = pow(color, vec3(1./2.2)); // gamma correct
    
    gl_FragColor = vec4(color, 1.);
}
