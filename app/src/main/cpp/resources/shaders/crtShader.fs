#version 330

in vec2 fragTexCoord;
in vec4 fragColor;

uniform sampler2D texture0;
uniform float time;
uniform vec2 resolution;
uniform float control_warp;

out vec4 finalColor;

vec2 curve(vec2 uv)
{
	uv = (uv - 0.5) * 1.2;

	uv *= 1.1;

	uv.x *= 1.0 + pow(abs(uv.y)/5.0, 2.0);
	uv.y *= 1.0 + pow(abs(uv.x)/control_warp, 2.0);

	uv = (uv/2.0) + 0.5;
	// uv = uv*0.92 + 0.04;

	return uv;
}

void main()
{
	vec2 uv = fragTexCoord;

	uv = curve(uv);

	vec3 col;

	// Chromatic aberration
	col.r = texture(texture0, vec2(uv.x + 0.003, uv.y)).r;
	col.g = texture(texture0, vec2(uv.x + 0.000, uv.y)).g;
	col.b = texture(texture0, vec2(uv.x - 0.003, uv.y)).b;

	// Border clipping
	if (uv.x < 0.0 || uv.x > 1.0 ||
			uv.y < 0.0 || uv.y > 1.0)
	{
		col = vec3(0.0);
	}

	// Vignette
	col *= 0.5 +
		0.5 *
		16.0 *
		uv.x *
		uv.y *
		(1.0 - uv.x) *
		(1.0 - uv.y);

	col *= vec3(0.95, 1.05, 0.95);

	// Scanlines
	col *= 0.9 + 0.1*sin(10.0*time + uv.y*700.0);

	// Flicker
	col *= 0.99 + 0.01*sin(110.0*time);

	finalColor = vec4(col, 1.0);
}

