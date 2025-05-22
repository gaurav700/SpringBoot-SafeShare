package com.shareApp.config;

import com.shareApp.dto.SignUpDTO;
import com.shareApp.entities.User;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.typeMap(SignUpDTO.class, User.class).addMappings(mapper -> {
            mapper.skip(User::setId);  // don't map id field
        });

        return modelMapper;
    }
}
