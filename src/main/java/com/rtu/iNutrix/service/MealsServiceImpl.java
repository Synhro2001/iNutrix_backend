package com.rtu.iNutrix.service;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.rtu.iNutrix.models.DTO.Meals.*;
import com.rtu.iNutrix.models.DTO.Products.ProductDTO;
import com.rtu.iNutrix.models.DTO.UserDataDTO;
import com.rtu.iNutrix.service.interfaces.MealsService;
import com.rtu.iNutrix.service.interfaces.ProductsService;
import com.rtu.iNutrix.service.interfaces.UserDataService;
import com.rtu.iNutrix.utilities.constants.LookUpConstants;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Variable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MealsServiceImpl implements MealsService {

    @Autowired
    private UserDataService _userDataService;

    @Autowired
    private ProductsService _productService;

    @Override
    public DietDayMetaData getDietDayMetadata() throws IllegalAccessException {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");

        Nutrients nutrients  = _getNeededNutrients();

        List<ProductDTO> products = _productService.getAllProducts();

        HashMap<ProductDTO, MPVariable> map = new HashMap<>();


        for(ProductDTO product : products){
            map.put(product,solver.makeNumVar(0.0,4,product.getName()));
        }


        //Sugar Constrains
        _addCustomConstraintForProductGroup(solver,map,"Sugar",0,1,LookUpConstants.LookUp_ProductGroup_ConfectioneryProducts);
        //Fruit Constraints
        _addCustomConstraintForProductGroup(solver,map,"Fruits",0,2,LookUpConstants.LookUp_ProductGroup_FruitsAndBerries);
        //Meat Constraints
       // _addCustomConstraintForProductGroup(solver,map,"Meat",1,3,LookUpConstants.LookUp_ProductGroup_MeatProducts);
        //Cereal Constraints
      //  _addCustomConstraintForProductGroup(solver,map,"Cereal Products",1,4.5,LookUpConstants.LookUp_ProductGroup_CerealProducts);

        MPConstraint protein = solver.makeConstraint(nutrients.getProtein().getMinimumValue(),nutrients.getProtein().getMaximumValue(),"Protein");
        MPConstraint carbs = solver.makeConstraint(nutrients.getCarbohydrates().getMinimumValue(),nutrients.getCarbohydrates().getMaximumValue(),"Carbs");
        MPConstraint fat = solver.makeConstraint(nutrients.getFat().getMinimumValue(),nutrients.getFat().getMaximumValue(),"Fat");
        MPConstraint kcal = solver.makeConstraint(nutrients.getKcal().getMinimumValue(),nutrients.getKcal().getMaximumValue(),"Kcal");
        MPConstraint A = solver.makeConstraint(nutrients.getA().getMinimumValue(),nutrients.getA().getMaximumValue(),"A");
        MPConstraint B1 = solver.makeConstraint(nutrients.getB1().getMinimumValue(),nutrients.getB1().getMaximumValue(),"B1");
        MPConstraint B2 = solver.makeConstraint(nutrients.getB2().getMinimumValue(),nutrients.getB2().getMaximumValue(),"B2");
        MPConstraint PP = solver.makeConstraint(nutrients.getPP().getMinimumValue(),nutrients.getPP().getMaximumValue(),"PP");
        MPConstraint C = solver.makeConstraint(nutrients.getC().getMinimumValue(),nutrients.getC().getMaximumValue(),"C");
        MPConstraint Ca = solver.makeConstraint(nutrients.getCa().getMinimumValue(),nutrients.getCa().getMaximumValue(),"Ca");
        MPConstraint P = solver.makeConstraint(nutrients.getP().getMinimumValue(),nutrients.getP().getMaximumValue(),"P");
        MPConstraint Fe = solver.makeConstraint(nutrients.getFe().getMinimumValue(),nutrients.getFe().getMaximumValue(),"Fe");


        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            protein.setCoefficient(entry.getValue(),entry.getKey().getProtein());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            carbs.setCoefficient(entry.getValue(),entry.getKey().getCarbohydrates());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            fat.setCoefficient(entry.getValue(),entry.getKey().getFat());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            kcal.setCoefficient(entry.getValue(),entry.getKey().getKcal());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
           A.setCoefficient(entry.getValue(),entry.getKey().getA());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            B1.setCoefficient(entry.getValue(),entry.getKey().getB1());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            B2.setCoefficient(entry.getValue(),entry.getKey().getB2());
        }
        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            PP.setCoefficient(entry.getValue(),entry.getKey().getPP());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            C.setCoefficient(entry.getValue(),entry.getKey().getC());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            Ca.setCoefficient(entry.getValue(),entry.getKey().getCa());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            P.setCoefficient(entry.getValue(),entry.getKey().getP());
        }

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            Fe.setCoefficient(entry.getValue(),entry.getKey().getFe());
        }

        MPObjective objective = solver.objective();

        int min = 1;
        int max = 10;

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            objective.setCoefficient(entry.getValue(),(int)Math.floor(Math.random()*(max-min+1)+min));
        }



        objective.setMaximization();

        solver.solve();


        List<DailyProduct> mealProducts = new ArrayList<>();

        DietDayMetaData metadata = new DietDayMetaData(nutrients);

        metadata.setProducts(mealProducts);

        for (Map.Entry<ProductDTO, MPVariable> entry : map.entrySet()) {
            if(entry.getValue().solutionValue() > 0){
                ProductDTO product = entry.getKey();
                double value = entry.getValue().solutionValue();

                mealProducts.add(new DailyProduct(product,value));

                metadata.setNutrientAmount(product,value);

            }
        }

        return metadata;

    }

    @Override
    public List<DietDay> getDiet(int days) throws IllegalAccessException {

        List<DietDay> diet = new ArrayList<>();
        ZonedDateTime today = ZonedDateTime.now(ZoneOffset.UTC);


        for(int i = 0; i<days;i++){
            DietDay dietDay = new DietDay();

            ZonedDateTime date =  today.plusDays(i);

            dietDay.setDate(date);
            dietDay.setDietDayMetadata(getDietDayMetadata());

            diet.add(dietDay);

        }

        return diet;
    }

    @Override
    public List<MealDTO> getMealsForDay(List<DailyProduct> products) {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");


        //Breakfast 35% of calories
        //Lunch 45% of calories
        //20% of calories


        return null;
    }

    private void _addCustomConstraintForProductGroup(MPSolver solver ,HashMap<ProductDTO,MPVariable> variables, String name,double lowerValue,double upperValue,UUID productGroup){
        Map<ProductDTO, MPVariable> filtredVariables = variables.entrySet().stream().filter(x->x.getKey().getProductGroup().getId().equals(productGroup)).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));

        MPConstraint constraint = solver.makeConstraint(lowerValue,upperValue,name);

        for (Map.Entry<ProductDTO, MPVariable> entry :filtredVariables.entrySet()) {
            constraint.setCoefficient(entry.getValue(),1);
        }
    }



    private Nutrients _getNeededNutrients(){
        double caloriesNeededPerDay = _getAMRrate() * _getMBR();

        //Carbs 4 calories per gram
        //Protein 4 calories per gram
        // Fat 9 calories per gram

        double carbsAmount = (caloriesNeededPerDay*0.60)/4;
        double proteinAmount = (caloriesNeededPerDay * 0.25)/4;
        double fatAmount = (caloriesNeededPerDay * 0.15)/9;



        Nutrient carbs = new Nutrient("Carbs",carbsAmount * 0.9,carbsAmount * 1.1);
        Nutrient protein = new Nutrient("Protein",proteinAmount * 0.9, proteinAmount * 1.1);
        Nutrient fat = new Nutrient("Fat",fatAmount * 0.9,fatAmount * 1.1);
        Nutrient kCal = new Nutrient("kCal",caloriesNeededPerDay * 0.9,caloriesNeededPerDay * 1.1);
        return Nutrients.requiredNutrients
                (_userDataService.getUserData().getGender(),
                        protein,
                        fat,
                        carbs,
                       kCal
                        );
    }




    private double _getAMRrate(){

        UUID userActivityLevel = _userDataService.getUserData().getActivityLevel();

        if(LookUpConstants.LookUp_ActivityLevel_Sedentary.equals(userActivityLevel)){
            return 1.2;
        }else if(LookUpConstants.LookUp_ActivityLevel_LightlyActive.equals(userActivityLevel)){
            return 1.375;
        }else if(LookUpConstants.LookUp_ActivityLevel_ModeratelyActive.equals(userActivityLevel)){
            return 1.55;
        }else if(LookUpConstants.LookUp_ActivityLevel_Active.equals(userActivityLevel)){
            return 1.725;
        }else{
            return 1.9;
        }
    }

    private double _getMBR(){
        UserDataDTO userData = _userDataService.getUserData();

        if(userData.getGender() == 'M'){
            return 66.47+ 13.75 * userData.getBodyWeight()+5.003 * userData.getHeight() - 6.755 * userData.getAge();
        }else{
            return 655.1+ 9.563 * userData.getBodyWeight()+1.85 * userData.getHeight() - 4.676 * userData.getAge();
        }
    }


}
