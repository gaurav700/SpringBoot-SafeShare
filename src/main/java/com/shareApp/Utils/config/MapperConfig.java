package com.shareApp.Utils.config;

import com.shareApp.Authentication.dto.SignUpDTO;
import com.shareApp.Authentication.entities.User;
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
