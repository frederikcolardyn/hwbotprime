#ifdef GL_ES
precision mediump float;
#endif

const float PI = 3.141592653589793;
const float HALF_PI = PI / 2.0;
const float DEG_TO_RAD = PI / 180.0;
const float RADIUS = 850.0;
const float EPSILON = 0.000001;

uniform vec3 windowDimensions;
uniform vec3 sunDirection;
uniform vec3 zenithData; //zenithX, zenithY, zenithLuminance
uniform float perezLuminance[5];
uniform float perezX[5];
uniform float perezY[5];
uniform vec3 colourCorrection; //exposure, overcast, gammaCorrection

float perezFunctionO2(float perezCoeffs[5], float cosTheta,
			float gamma, float cosGamma2, float zenithValue) {
	return zenithValue
			* (1.0 + perezCoeffs[0]
					* exp(perezCoeffs[1] * cosTheta))
			* (1.0 + perezCoeffs[2] * exp(perezCoeffs[3] * gamma) + perezCoeffs[4]
					* cosGamma2);
}

vec3 convertXYZtoRGB(float xVar, float yVar, float zVar) {
	return vec3( 3.240479 * xVar - 1.537150 * yVar - 0.498530 * zVar,
	-0.969256 * xVar + 1.875991 * yVar + 0.041556 * zVar,
	0.055648 * xVar - 0.204043 * yVar + 1.057311 * zVar);
}

vec3 convertRGBtoHSV(vec3 RGB) {
	vec3 HSV = vec3(0.0, 0.0, 0.0);
    HSV.z = max(RGB.r, max(RGB.g, RGB.b));
    float M = min(RGB.r, min(RGB.g, RGB.b));
    float C = HSV.z - M;
    if (C != 0.0)
    {
        HSV.y = C / HSV.z;
        vec3 Delta = (HSV.z - RGB) / C;
        Delta.rgb -= Delta.brg;
        Delta.rg += vec2(2,4);
        if (RGB.r >= HSV.z)
            HSV.x = Delta.b;
        else if (RGB.g >= HSV.z)
            HSV.x = Delta.r;
        else
            HSV.x = Delta.g;
        HSV.x = fract(HSV.x / 6.0);
    }
    return HSV;
}

vec3 doHue(float H)
{
    float R = abs(H * 6.0 - 3.0) - 1.0;
    float G = 2.0 - abs(H * 6.0 - 2.0);
    float B = 2.0 - abs(H * 6.0 - 4.0);
   return vec3(clamp(R,0.0,1.0), clamp(G,0.0,1.0), clamp(B,0.0,1.0));
}

vec3 convertHSVtoRGB(vec3 HSV)
{
    return ((doHue(HSV.x) - 1.0) * HSV.y + 1.0) * HSV.z;
}

void main(void)
{
// disable this?
	float r = gl_FragCoord.x / windowDimensions.x;
	float g = gl_FragCoord.y / windowDimensions.y;
	gl_FragColor = vec4(r, g, 0.0, 1.0);
// end disable this
	
	float thetaRad = DEG_TO_RAD * (90.0 - (gl_FragCoord.y / windowDimensions.y) * 90.0);
	float phiRad = DEG_TO_RAD * ((gl_FragCoord.x / windowDimensions.x) * 180.0);
	
	vec3 vertex = normalize(vec3(
		cos(HALF_PI - thetaRad) * cos(phiRad),
		sin(HALF_PI - thetaRad),
		cos(HALF_PI - thetaRad) * sin(phiRad)
	));
	
	float gamma = acos(dot(vertex, sunDirection));
	
	float cosTheta = 1.0 / vertex.y;
	float cosGamma = cos(gamma);
	float cosGamma2 = cosGamma * cosGamma;
	
	// Compute x,y values
	float x_value = perezFunctionO2(perezX, cosTheta, gamma, cosGamma2, zenithData.x);
	float y_value = perezFunctionO2(perezY, cosTheta, gamma, cosGamma2, zenithData.y);
	
	// luminance(Y) for clear & overcast sky
	float yClear = perezFunctionO2(perezLuminance, cosTheta, gamma,
			cosGamma2, zenithData.z);
	float yOver = (1.0 + 2.0 * vertex.y) / 3.0;

	float _Y = mix(colourCorrection.y, yClear, yOver);
	float _X = (x_value / y_value) * _Y;
	float _Z = ((1.0 - x_value - y_value) / y_value) * _Y;
	
	vec3 colourRGB = convertXYZtoRGB(_X, _Y, _Z);
	vec3 colourHSV = convertRGBtoHSV(colourRGB);

	colourHSV.z *= colourCorrection.x;
	
	colourRGB = convertHSVtoRGB(colourHSV);
	
	colourRGB.x = pow(colourRGB.x, colourCorrection.z);
	colourRGB.y = pow(colourRGB.y, colourCorrection.z);
	colourRGB.z = pow(colourRGB.z, colourCorrection.z);
	
	gl_FragColor.rgb = colourRGB;
	//gl_FragColor.a = colourRGB.b;
	gl_FragColor.a = 1.0;
}
