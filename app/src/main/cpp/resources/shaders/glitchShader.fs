#version 330

in vec2 fragTexCoord;
in vec4 fragColor;

uniform sampler2D texture0;
uniform float time;

out vec4 finalColor;

float rand(vec2 co)
{
	return fract(
			sin(dot(co.xy, vec2(12.9898,78.233)))
			* 43758.5453
			);
}

void main()
{
	vec2 uv = fragTexCoord;

	float glitch =
		step(
				0.96,
				rand(vec2(
						floor(uv.y * 40.0),
						floor(time * 8.0)
						))
			);

	uv.x += glitch * 0.02;

	vec4 color =
		texture(texture0, uv);

	finalColor = color;
}
