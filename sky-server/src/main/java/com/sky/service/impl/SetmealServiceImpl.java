package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDto) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDto, setmeal);
        //向套餐表插入数据
        setmealMapper.insert(setmeal);
        //获取生成的套餐id
        Long setmealId = setmeal.getId();

        //获取套餐菜品关联关系数据
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //为每个套餐菜品关联关系设置套餐id
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        //向套餐菜品关联表插入数据,实现新增套餐
        setmealDishMapper.insertBatch(setmealDishes);
    }
    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }
    /**
     * 批量删除套餐，同时删除套餐和菜品的关联关系
     * @param ids
     */
    @Override
    @Transactional
    public void deleteWithDish(List<Long> ids) {
        //查询套餐状态，确定是否可用删除
        ids.forEach(id -> {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                // 套餐正在售卖中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });
            //删除套餐表中的数据
            setmealMapper.deleteByIds(ids);
            //删除套餐菜品关联表中的数据
            setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //查询套餐基本信息
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);

        //查询套餐对应的菜品信息
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }
    /**
     * 修改套餐，同时更新套餐和菜品的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void update(SetmealDTO setmealDto) {
        //更新套餐表中的数据
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDto, setmeal);
        setmealMapper.update(setmeal);

        //获取套餐id
        Long setmealId = setmeal.getId();
        //删除套餐菜品关联表中的数据 由于deleteBySetmealIds方法接收的是List<Long>类型的参数，所以这里使用Collections.singletonList(setmealId)将setmealId转换为一个只包含一个元素的List
        //如果是多个可以参考dishIds的写法，使用Arrays.asList(setmealId1, setmealId2, ...)来创建一个包含多个元素的List
        setmealDishMapper.deleteBySetmealIds(Collections.singletonList(setmealId));
        //新增套餐菜品关联表中的数据
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //为每个套餐菜品关联关系设置套餐id
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        //向套餐菜品关联表插入数据,实现修改套餐
        setmealDishMapper.insertBatch(setmealDishes);
    }
    /**
     * 起售或者停售套餐
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if(status == StatusConstant.ENABLE){
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList != null && dishList.size()>0){
               dishList.forEach(dish -> {
                     if(dish.getStatus() == StatusConstant.DISABLE){
                          throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                     }
                });
                }
        }
        //更新套餐状态
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
        }

    }
