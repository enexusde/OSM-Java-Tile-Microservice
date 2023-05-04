/**
 *  _____         _         _           _      _____         _              _____ _ _         
 * |   __|___ ___|_|___ ___| |_ ___ ___| |_   |  |  |___ ___| |_ ___ ___   |_   _|_| |___ ___ 
 * |__   | . |  _| |   | . | . | . | . |  _|  |  |  | -_|  _|  _| . |  _|    | | | | | -_|_ -|
 * |_____|  _|_| |_|_|_|_  |___|___|___|_|     \___/|___|___|_| |___|_|      |_| |_|_|___|___|
 *       |_|           |___| Copyright (c) 2023 Peter Rader
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.                                                                  
 */
package de.e_nexus.web.osm;

import javax.inject.Named;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Named
@SpringBootApplication
@EnableAutoConfiguration
@EnableAsync
@EnableJpaRepositories(basePackages = "de.e_nexus.web.osm.jpa.repo")
@ComponentScan("de.e_nexus.web.osm")
@EntityScan("de.e_nexus.web.osm.jpa.entitiesx")
@EnableWebMvc
public class AppConfig implements WebMvcConfigurer {
	protected AppConfig() {
	}

	@Override
	public void addResourceHandlers(final ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/web/**").addResourceLocations("/");
	}

	public static void main(final String... args) {
		SpringApplication.run(AppConfig.class, args);
	}
}
