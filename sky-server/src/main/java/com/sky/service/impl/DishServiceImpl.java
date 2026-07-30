package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    /**
     * 新增菜品，同时保存口味数据
     *
     * @param dishDTO
     */
    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 保存菜品基本信息到菜品表
        dishMapper.insert(dish);
        // 获取菜品id, 因为dishDTO中没有id, 需要从dish中获取
        Long dishId = dish.getId();
        // 保存菜品口味数据到菜品口味表
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors !=null && flavors.size()>0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }
}
